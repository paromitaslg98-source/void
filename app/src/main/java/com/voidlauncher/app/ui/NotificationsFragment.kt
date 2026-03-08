package com.voidlauncher.app.ui

import android.content.pm.PackageManager
import android.os.Bundle
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.content.Intent
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import com.voidlauncher.app.R
import com.voidlauncher.app.data.NotificationGroup
import com.voidlauncher.app.databinding.FragmentNotificationsBinding
import com.voidlauncher.app.databinding.RowNotificationGroupBinding
import com.voidlauncher.app.helper.NotificationService

class NotificationsFragment : Fragment() {

    private var _binding: FragmentNotificationsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentNotificationsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = NotificationAdapter()
        binding.rvNotifications.layoutManager = LinearLayoutManager(requireContext())
        binding.rvNotifications.adapter = adapter

        NotificationService.notificationsLiveData.observe(viewLifecycleOwner) { groups ->
            val hasNotifications = groups.isNotEmpty()
            binding.rvNotifications.visibility = if (hasNotifications) View.VISIBLE else View.GONE
            binding.tvNoNotifications.visibility = if (hasNotifications) View.GONE else View.VISIBLE
            binding.tvClearAll.visibility = if (hasNotifications) View.VISIBLE else View.GONE
            
            adapter.submitList(groups)
        }

        binding.tvClearAll.setOnClickListener {
            // Cancel all notifications through an intent or bound service call
        }

        binding.tvNoNotifications.setOnClickListener {
            if (!hasNotificationAccess()) {
                startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            }
        }
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

    inner class NotificationAdapter : RecyclerView.Adapter<NotificationAdapter.ViewHolder>() {

        private var items = listOf<NotificationGroup>()
        private val pm: PackageManager = requireContext().packageManager

        fun submitList(newItems: List<NotificationGroup>) {
            items = newItems
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = RowNotificationGroupBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.bind(item)
        }

        override fun getItemCount() = items.size

        inner class ViewHolder(private val binding: RowNotificationGroupBinding) : RecyclerView.ViewHolder(binding.root) {
            fun bind(group: NotificationGroup) {
                try {
                    val appInfo = pm.getApplicationInfo(group.packageName, 0)
                    binding.tvAppName.text = pm.getApplicationLabel(appInfo)
                    binding.ivAppIcon.setImageDrawable(pm.getApplicationIcon(appInfo))
                } catch (e: Exception) {
                    binding.tvAppName.text = group.packageName
                }

                val latest = group.notifications.firstOrNull()?.notification
                binding.tvTitle.text = latest?.extras?.getString(android.app.Notification.EXTRA_TITLE)
                binding.tvText.text = latest?.extras?.getString(android.app.Notification.EXTRA_TEXT)

                val timeString = DateUtils.getRelativeTimeSpanString(group.latestTimestamp, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS)
                binding.tvTime.text = timeString

                if (group.childCount > 1) {
                    binding.tvCount.visibility = View.VISIBLE
                    binding.tvCount.text = "+${group.childCount - 1} more"
                } else {
                    binding.tvCount.visibility = View.GONE
                }

                binding.root.setOnClickListener {
                    // Open the notification intent
                    try {
                        latest?.contentIntent?.send()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }
}
