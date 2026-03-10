package com.voidlauncher.app.ui

import android.annotation.SuppressLint
import android.app.Notification
import android.content.Intent
import android.content.pm.PackageManager
import android.animation.LayoutTransition
import android.os.Bundle
import android.provider.Settings
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationManagerCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.voidlauncher.app.R
import com.voidlauncher.app.data.NotificationGroup
import com.voidlauncher.app.databinding.FragmentNotificationsBinding
import com.voidlauncher.app.databinding.RowNotificationGroupBinding
import com.voidlauncher.app.helper.NotificationService
import com.voidlauncher.app.listener.OnSwipeTouchListener

class NotificationsFragment : Fragment() {

    private var _binding: FragmentNotificationsBinding? = null
    private val binding get() = _binding!!
    private var latestLiveGroups: List<NotificationGroup> = emptyList()
    private var pendingUndoGroups: List<NotificationGroup>? = null
    private var pendingUndoExpandedGroups: Set<String> = emptySet()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentNotificationsBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = NotificationAdapter()
        binding.rvNotifications.layoutManager = LinearLayoutManager(requireContext())
        binding.rvNotifications.itemAnimator = DefaultItemAnimator().apply {
            addDuration = 300
            removeDuration = 300
            changeDuration = 300
            moveDuration = 250
            supportsChangeAnimations = false
        }
        binding.rvNotifications.adapter = adapter

        NotificationService.notificationsLiveData.observe(viewLifecycleOwner) { groups ->
            latestLiveGroups = groups
            if (pendingUndoGroups == null) {
                renderNotifications(groups, adapter)
            }
        }

        binding.tvClearAll.setOnClickListener {
            val currentGroups = adapter.currentList
            if (currentGroups.isEmpty()) {
                return@setOnClickListener
            }

            pendingUndoGroups = currentGroups.toList()
            pendingUndoExpandedGroups = adapter.getExpandedGroupKeys()

            renderNotifications(emptyList(), adapter)
            NotificationManagerCompat.from(requireContext()).cancelAll()

            Snackbar.make(binding.root, R.string.notifications_cleared, 5000)
                .setAction(R.string.undo) {
                    val restoredGroups = mergeUndoSnapshotWithLiveState(
                        snapshot = pendingUndoGroups.orEmpty(),
                        live = latestLiveGroups
                    )
                    pendingUndoGroups = null
                    adapter.setExpandedGroupKeys(pendingUndoExpandedGroups)
                    renderNotifications(restoredGroups, adapter)
                    pendingUndoExpandedGroups = emptySet()
                }
                .addCallback(object : Snackbar.Callback() {
                    override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                        if (event != DISMISS_EVENT_ACTION) {
                            clearUndoState()
                            renderNotifications(latestLiveGroups, adapter)
                        }
                    }
                })
                .show()
        }

        binding.tvNoNotifications.setOnClickListener {
            if (!hasNotificationAccess()) {
                startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            }
        }

        // Reverse swipe: swipe left (finger right-to-left) → go back to home
        view.setOnTouchListener(object : OnSwipeTouchListener(requireContext()) {
            override fun onSwipeLeft() {
                findNavController().popBackStack()
            }
        })
    }

    private fun renderNotifications(groups: List<NotificationGroup>, adapter: NotificationAdapter) {
        val hasNotifications = groups.isNotEmpty()
        binding.rvNotifications.visibility = if (hasNotifications) View.VISIBLE else View.GONE
        binding.tvNoNotifications.visibility = if (hasNotifications) View.GONE else View.VISIBLE
        binding.tvClearAll.visibility = if (hasNotifications) View.VISIBLE else View.GONE
        adapter.submitList(groups)
    }

    private fun mergeUndoSnapshotWithLiveState(
        snapshot: List<NotificationGroup>,
        live: List<NotificationGroup>
    ): List<NotificationGroup> {
        if (snapshot.isEmpty()) {
            return live
        }

        val merged = LinkedHashMap<String, NotificationGroup>()
        snapshot.forEach { merged[it.groupKey] = it }
        live.forEach { merged[it.groupKey] = it }

        return merged.values.sortedWith(
            compareByDescending<NotificationGroup> { it.childCount }
                .thenByDescending { it.latestTimestamp }
        )
    }

    override fun onResume() {
        super.onResume()
        if (!hasNotificationAccess()) {
            binding.rvNotifications.visibility = View.GONE
            binding.tvClearAll.visibility = View.GONE
            binding.tvNoNotifications.visibility = View.VISIBLE
            binding.tvNoNotifications.text = "Notification access required. Tap to enable."
        } else {
            binding.tvNoNotifications.text = getString(R.string.no_notifications)
        }
    }

    private fun hasNotificationAccess(): Boolean {
        return NotificationManagerCompat.getEnabledListenerPackages(requireContext())
            .contains(requireContext().packageName)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    inner class NotificationAdapter : ListAdapter<NotificationGroup, NotificationAdapter.ViewHolder>(DIFF_CALLBACK) {

        private val pm: PackageManager = requireContext().packageManager
        private val expandedGroups = mutableSetOf<String>()

        fun getExpandedGroupKeys(): Set<String> = expandedGroups.toSet()

        fun setExpandedGroupKeys(groupKeys: Set<String>) {
            expandedGroups.clear()
            expandedGroups.addAll(groupKeys)
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = RowNotificationGroupBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(getItem(position))
        }

        inner class ViewHolder(private val binding: RowNotificationGroupBinding) : RecyclerView.ViewHolder(binding.root) {
            fun bind(item: NotificationGroupItem) {
                val group = item.group
                // App info
                try {
                    val appInfo = pm.getApplicationInfo(group.packageName, 0)
                    binding.tvAppName.text = pm.getApplicationLabel(appInfo)
                    binding.ivAppIcon.setImageDrawable(pm.getApplicationIcon(appInfo))
                } catch (e: Exception) {
                    binding.tvAppName.text = group.packageName
                }

                // Latest notification preview
                val latest = group.notifications.firstOrNull()?.notification
                binding.tvTitle.text = latest?.extras?.getString(Notification.EXTRA_TITLE) ?: ""
                binding.tvText.text = latest?.extras?.getString(Notification.EXTRA_TEXT) ?: ""

                val timeString = DateUtils.getRelativeTimeSpanString(
                    group.latestTimestamp, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS
                )
                binding.tvTime.text = timeString

                // Count badge and expand arrow
                val isExpanded = item.isExpanded
                
                binding.tvCount.visibility = View.VISIBLE
                binding.tvCount.text = "${group.childCount}"
                
                if (group.childCount > 1) {
                    binding.ivExpand.visibility = View.VISIBLE
                    binding.ivExpand.rotation = if (isExpanded) 180f else 0f
                } else {
                    binding.ivExpand.visibility = View.GONE
                }

                // Expand/collapse children
                if (isExpanded && group.childCount > 1) {
                    binding.llLatestPreview.visibility = View.GONE
                    binding.llChildren.visibility = View.VISIBLE
                    populateChildren(binding.llChildren, group)
                } else {
                    binding.llLatestPreview.visibility = View.VISIBLE
                    binding.llChildren.visibility = View.GONE
                    binding.llChildren.removeAllViews()
                }

                // Click handler: toggle expand or open notification
                binding.llGroupHeader.setOnClickListener {
                    if (group.childCount > 1) {
                        if (item.isExpanded) {
                            expandedGroups.remove(group.groupKey)
                        } else {
                            expandedGroups.add(group.groupKey)
                        }
                        submitListInternal()
                    } else {
                        try {
                            latest?.contentIntent?.send()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }

                binding.ivExpand.animate()
                    .rotation(if (isExpanded) 180f else 0f)
                    .setDuration(220)
                    .setInterpolator(AccelerateDecelerateInterpolator())
                    .start()

                binding.llChildren.layoutTransition = LayoutTransition().apply {
                    setAnimateParentHierarchy(false)
                    setDuration(LayoutTransition.APPEARING, 220)
                    setDuration(LayoutTransition.DISAPPEARING, 220)
                    setDuration(LayoutTransition.CHANGE_APPEARING, 220)
                    setDuration(LayoutTransition.CHANGE_DISAPPEARING, 220)
                    setDuration(LayoutTransition.CHANGING, 220)
                    setInterpolator(LayoutTransition.APPEARING, AccelerateDecelerateInterpolator())
                    setInterpolator(LayoutTransition.DISAPPEARING, AccelerateDecelerateInterpolator())
                }
                
                // Click handler: open notification from preview
                binding.llLatestPreview.setOnClickListener {
                    try {
                        latest?.contentIntent?.send()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            private fun populateChildren(container: LinearLayout, group: NotificationGroup) {
                container.removeAllViews()
                val inflater = LayoutInflater.from(container.context)
                for (sbn in group.notifications) {
                    val childView = inflater.inflate(R.layout.row_notification_child, container, false)
                    val title = sbn.notification.extras.getString(Notification.EXTRA_TITLE) ?: ""
                    val text = sbn.notification.extras.getString(Notification.EXTRA_TEXT) ?: ""
                    val time = DateUtils.getRelativeTimeSpanString(
                        sbn.postTime, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS
                    )

                    childView.findViewById<TextView>(R.id.tvChildTitle).text = title
                    childView.findViewById<TextView>(R.id.tvChildText).text = text
                    childView.findViewById<TextView>(R.id.tvChildTime).text = time

                    childView.setOnClickListener {
                        try {
                            sbn.notification.contentIntent?.send()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }

                    container.addView(childView)
                }
            }
        }

        companion object {
            private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<NotificationGroup>() {
                override fun areItemsTheSame(oldItem: NotificationGroup, newItem: NotificationGroup): Boolean {
                    return oldItem.groupKey == newItem.groupKey
                }

                override fun areContentsTheSame(oldItem: NotificationGroup, newItem: NotificationGroup): Boolean {
                    return oldItem == newItem
                }
            }
        }
    }
}
