package com.voidlauncher.app.ui

import android.content.Context
import android.os.Build
import android.os.UserHandle
import android.os.UserManager
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Filter
import android.widget.Filterable
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.voidlauncher.app.R
import com.voidlauncher.app.data.AppModel
import com.voidlauncher.app.data.Constants
import com.voidlauncher.app.databinding.AdapterAppDrawerBinding
import com.voidlauncher.app.databinding.AdapterPrivateSpaceBinding
import com.voidlauncher.app.databinding.AdapterSectionHeaderBinding
import com.voidlauncher.app.helper.hideKeyboard
import com.voidlauncher.app.helper.isSystemApp
import com.voidlauncher.app.helper.showKeyboard
import java.text.Normalizer

/** Sealed item for the drawer list. */
sealed class DrawerItem {
    data class AppItem(val appModel: AppModel) : DrawerItem()
    object PrivateSpace : DrawerItem()
    data class SectionHeader(val title: String) : DrawerItem()
}

class AppDrawerAdapter(
    private var flag: Int,
    private val appLabelGravity: Int,
    private val appClickListener: (AppModel) -> Unit,
    private val appInfoListener: (AppModel) -> Unit,
    private val appDeleteListener: (AppModel) -> Unit,
    private val appHideListener: (AppModel, Int) -> Unit,
    private val appRenameListener: (AppModel, String) -> Unit,
    private val privateSpaceClickListener: (() -> Unit)? = null,
) : ListAdapter<DrawerItem, RecyclerView.ViewHolder>(DIFF_CALLBACK), Filterable {

    companion object {
        const val VIEW_TYPE_APP = 0
        const val VIEW_TYPE_PRIVATE_SPACE = 1
        const val VIEW_TYPE_SECTION_HEADER = 2

        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<DrawerItem>() {
            override fun areItemsTheSame(oldItem: DrawerItem, newItem: DrawerItem): Boolean =
                when {
                    oldItem is DrawerItem.AppItem && newItem is DrawerItem.AppItem -> {
                        val o = oldItem.appModel; val n = newItem.appModel
                        when {
                            o is AppModel.App && n is AppModel.App ->
                                o.appPackage == n.appPackage && o.user == n.user
                            o is AppModel.PinnedShortcut && n is AppModel.PinnedShortcut ->
                                o.shortcutId == n.shortcutId && o.user == n.user
                            else -> false
                        }
                    }
                    oldItem is DrawerItem.PrivateSpace && newItem is DrawerItem.PrivateSpace -> true
                    oldItem is DrawerItem.SectionHeader && newItem is DrawerItem.SectionHeader ->
                        oldItem.title == newItem.title
                    else -> false
                }

            override fun areContentsTheSame(oldItem: DrawerItem, newItem: DrawerItem) =
                oldItem == newItem
        }
    }

    private var isBangSearch = false
    private val appFilter = createAppFilter()
    private val myUserHandle = android.os.Process.myUserHandle()
    
    // Tracks injected apps from private space so they persist across search filtering
    private var privateAppsList: List<AppModel> = emptyList()
    // Remembers the last query to re-filter gracefully when private space state changes
    private var currentQuery: CharSequence = ""

    var appsList: MutableList<AppModel> = mutableListOf()
    var appFilteredList: MutableList<AppModel> = mutableListOf()

    // Controls whether the Private Space synthetic row is injected into results
    var showPrivateSpaceItem: Boolean = false

    override fun getItemViewType(position: Int): Int = when (getItem(position)) {
        is DrawerItem.PrivateSpace -> VIEW_TYPE_PRIVATE_SPACE
        is DrawerItem.SectionHeader -> VIEW_TYPE_SECTION_HEADER
        else -> VIEW_TYPE_APP
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        when (viewType) {
            VIEW_TYPE_PRIVATE_SPACE -> PrivateSpaceViewHolder(
                AdapterPrivateSpaceBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            )
            VIEW_TYPE_SECTION_HEADER -> SectionHeaderViewHolder(
                AdapterSectionHeaderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            )
            else -> AppViewHolder(
                AdapterAppDrawerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            )
        }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is DrawerItem.PrivateSpace ->
                (holder as PrivateSpaceViewHolder).bind(privateSpaceClickListener)
            is DrawerItem.SectionHeader ->
                (holder as SectionHeaderViewHolder).bind(item.title)
            is DrawerItem.AppItem -> {
                try {
                    (holder as AppViewHolder).bind(
                        flag, appLabelGravity, myUserHandle, item.appModel,
                        appClickListener, appDeleteListener, appInfoListener,
                        appHideListener, appRenameListener
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    override fun getFilter(): Filter = appFilter

    private fun createAppFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(charSearch: CharSequence?): FilterResults {
                currentQuery = charSearch ?: ""
                isBangSearch = charSearch?.startsWith("!") ?: false

                val combinedTarget = appsList + privateAppsList
                val filtered: MutableList<AppModel> = if (charSearch.isNullOrBlank()) combinedTarget.toMutableList()
                else combinedTarget.filter { app ->
                    appLabelMatches(app.appLabel, charSearch)
                } as MutableList<AppModel>

                return FilterResults().apply { values = filtered }
            }

            @Suppress("UNCHECKED_CAST")
            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                results?.values?.let {
                    val filtered = it as MutableList<AppModel>
                    appFilteredList = filtered

                    // Build DrawerItem list, optionally prepending the Private Space synthetic row
                    val drawerItems = mutableListOf<DrawerItem>()
                    if (showPrivateSpaceItem) {
                        drawerItems.add(DrawerItem.PrivateSpace)
                    }
                    filtered.forEach { app -> drawerItems.add(DrawerItem.AppItem(app)) }

                    submitList(drawerItems)
                }
            }
        }
    }

    private fun appLabelMatches(appLabel: String, charSearch: CharSequence): Boolean {
        return (appLabel.contains(charSearch.trim(), true) ||
                Normalizer.normalize(appLabel, Normalizer.Form.NFD)
                    .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
                    .replace(Regex("[-_+,. ]"), "")
                    .contains(charSearch, true))
    }

    fun setAppList(appsList: MutableList<AppModel>) {
        // Add empty app for bottom padding
        appsList.add(
            AppModel.App(
                appLabel = "", key = null, appPackage = "",
                activityClassName = "", isNew = false,
                user = android.os.Process.myUserHandle()
            )
        )
        this.appsList = appsList
        this.appFilteredList = appsList
        val drawerItems = appsList.map { DrawerItem.AppItem(it) }.toMutableList<DrawerItem>()
        submitList(drawerItems)
    }

    fun launchFirstInList() {
        val firstApp = getCurrentList()
            .firstOrNull { it is DrawerItem.AppItem && (it as DrawerItem.AppItem).appModel.appPackage.isNotEmpty() }
            as? DrawerItem.AppItem
        firstApp?.let { appClickListener(it.appModel) }
    }

    /**
     * Injects private space apps at the TOP of the list, preceded by a section header.
     * Called by the fragment after ACTION_PROFILE_AVAILABLE is received.
     */
    fun injectPrivateApps(privateApps: List<AppModel>) {
        if (privateApps.isEmpty()) return
        privateAppsList = privateApps
        appFilter.filter(currentQuery)
    }

    /** Called when ACTION_PROFILE_UNAVAILABLE fires (Private Space locks) */
    fun clearPrivateApps() {
        privateAppsList = emptyList()
        appFilter.filter(currentQuery)
    }

    // ─── ViewHolder: real app item ────────────────────────────────────────────
    class AppViewHolder(private val binding: AdapterAppDrawerBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(
            flag: Int,
            appLabelGravity: Int,
            myUserHandle: UserHandle,
            appModel: AppModel,
            clickListener: (AppModel) -> Unit,
            appDeleteListener: (AppModel) -> Unit,
            appInfoListener: (AppModel) -> Unit,
            appHideListener: (AppModel, Int) -> Unit,
            appRenameListener: (AppModel, String) -> Unit,
        ) = with(binding) {
            appHideLayout.visibility = View.GONE
            renameLayout.visibility = View.GONE
            appTitle.visibility = View.VISIBLE

            appTitle.text = buildString {
                append(appModel.appLabel)
                if (appModel.isNew) append(" ✦")
            }
            appTitle.gravity = appLabelGravity
            otherProfileIndicator.isVisible = appModel.user != myUserHandle

            appTitle.setOnClickListener { clickListener(appModel) }
            appTitle.setOnLongClickListener {
                if (appModel.appPackage.isNotEmpty()) {
                    appDelete.alpha = when (
                        appModel is AppModel.PinnedShortcut || !root.context.isSystemApp(appModel.appPackage)
                    ) {
                        true -> 1.0f
                        false -> 0.5f
                    }
                    appHide.text = if (flag == Constants.FLAG_HIDDEN_APPS)
                        root.context.getString(R.string.adapter_show)
                    else
                        root.context.getString(R.string.adapter_hide)
                    appTitle.visibility = View.INVISIBLE
                    appHide.alpha = when (appModel is AppModel.PinnedShortcut) {
                        true -> 0.5f; false -> 1.0f
                    }
                    appHideLayout.visibility = View.VISIBLE
                    appRename.isVisible = flag != Constants.FLAG_HIDDEN_APPS
                }
                true
            }

            appRename.setOnClickListener {
                if (appModel.appPackage.isNotEmpty()) {
                    etAppRename.hint = getAppName(etAppRename.context, appModel.appPackage)
                    etAppRename.setText(appModel.appLabel)
                    etAppRename.setSelectAllOnFocus(true)
                    renameLayout.visibility = View.VISIBLE
                    appHideLayout.visibility = View.GONE
                    etAppRename.showKeyboard()
                    etAppRename.imeOptions = EditorInfo.IME_ACTION_DONE
                }
            }
            etAppRename.onFocusChangeListener =
                View.OnFocusChangeListener { _, hasFocus ->
                    appTitle.visibility = if (hasFocus) View.INVISIBLE else View.VISIBLE
                }
            etAppRename.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    etAppRename.hint = getAppName(etAppRename.context, appModel.appPackage)
                }
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    etAppRename.hint = ""
                }
            })
            etAppRename.setOnEditorActionListener { _, actionCode, _ ->
                if (actionCode == EditorInfo.IME_ACTION_DONE) {
                    val renameLabel = etAppRename.text.toString().trim()
                    if (renameLabel.isNotBlank() && appModel.appPackage.isNotBlank()) {
                        appRenameListener(appModel, renameLabel)
                        renameLayout.visibility = View.GONE
                    }
                    return@setOnEditorActionListener true
                }
                false
            }
            tvSaveRename.setOnClickListener {
                etAppRename.hideKeyboard()
                val renameLabel = etAppRename.text.toString().trim()
                if (renameLabel.isNotBlank() && appModel.appPackage.isNotBlank()) {
                    appRenameListener(appModel, renameLabel)
                    renameLayout.visibility = View.GONE
                } else {
                    val pm = etAppRename.context.packageManager
                    appRenameListener(
                        appModel,
                        pm.getApplicationLabel(pm.getApplicationInfo(appModel.appPackage, 0)).toString()
                    )
                    renameLayout.visibility = View.GONE
                }
            }
            appInfo.setOnClickListener { appInfoListener(appModel) }
            appDelete.setOnClickListener { appDeleteListener(appModel) }
            appMenuClose.setOnClickListener {
                appHideLayout.visibility = View.GONE
                appTitle.visibility = View.VISIBLE
            }
            appRenameClose.setOnClickListener {
                renameLayout.visibility = View.GONE
                appTitle.visibility = View.VISIBLE
            }
            appHide.setOnClickListener { appHideListener(appModel, bindingAdapterPosition) }
        }

        private fun getAppName(context: Context, appPackage: String): String {
            val pm = context.packageManager
            return pm.getApplicationLabel(pm.getApplicationInfo(appPackage, 0)).toString()
        }
    }

    // ─── ViewHolder: section header ───────────────────────────────────────────
    class SectionHeaderViewHolder(private val binding: AdapterSectionHeaderBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(title: String) {
            binding.sectionTitle.text = title.uppercase()
        }
    }

    // ─── ViewHolder: Private Space synthetic row ──────────────────────────────
    class PrivateSpaceViewHolder(private val binding: AdapterPrivateSpaceBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(clickListener: (() -> Unit)?) {
            binding.root.setOnClickListener { clickListener?.invoke() }
        }
    }
}
