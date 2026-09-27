package com.example.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ScreenElement(
    val text: String,
    val contentDescription: String?,
    val viewId: String?,
    val isClickable: Boolean,
    val isEditable: Boolean,
    val bounds: Rect
)

data class ScreenContentReport(
    val packageName: String?,
    val elements: List<ScreenElement>,
    val fullTextSummary: String
)

class IrisAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "IrisAccessibility"

        private var _instance: IrisAccessibilityService? = null
        val instance: IrisAccessibilityService? get() = _instance

        private val _isServiceActive = MutableStateFlow(false)
        val isServiceActive: StateFlow<Boolean> = _isServiceActive.asStateFlow()

        fun isRunning(): Boolean = _instance != null
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        _instance = this
        _isServiceActive.value = true
        Log.i(TAG, "Iris Accessibility Service Connected & Ready for Hands-Free Device Control")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Active event listening for live window context
    }

    override fun onInterrupt() {
        Log.w(TAG, "Iris Accessibility Service Interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        if (_instance == this) {
            _instance = null
            _isServiceActive.value = false
        }
        Log.i(TAG, "Iris Accessibility Service Destroyed")
    }

    /**
     * Read current screen hierarchy and extract visible text, buttons, and editable controls
     */
    fun readScreen(): ScreenContentReport {
        val root = rootInActiveWindow ?: return ScreenContentReport(
            packageName = null,
            elements = emptyList(),
            fullTextSummary = "বর্তমানে কোনো সক্রিয় স্ক্রিন কনটেন্ট পাওয়া যায়নি (Accessibility Window empty)"
        )

        val elements = mutableListOf<ScreenElement>()
        traverseNode(root, elements)

        val pkg = root.packageName?.toString()
        val textSnippets = elements
            .mapNotNull { elem ->
                val label = elem.text.ifBlank { elem.contentDescription.orEmpty() }
                if (label.isNotBlank()) {
                    if (elem.isClickable) "[$label (বাটন)]" else label
                } else null
            }
            .distinct()
            .take(30)

        val summary = if (textSnippets.isEmpty()) {
            "স্ক্রিন দৃশ্যমান হলেও কোনো টেক্সট বা বাটন পাওয়া যায়নি。"
        } else {
            "বর্তমান অ্যাপ: ${pkg ?: "অজানা"}। দৃশ্যমান উপাদান: " + textSnippets.joinToString(", ")
        }

        return ScreenContentReport(
            packageName = pkg,
            elements = elements,
            fullTextSummary = summary
        )
    }

    private fun traverseNode(node: AccessibilityNodeInfo?, list: MutableList<ScreenElement>) {
        if (node == null || !node.isVisibleToUser) return

        val text = node.text?.toString()?.trim().orEmpty()
        val desc = node.contentDescription?.toString()?.trim()
        val viewId = node.viewIdResourceName
        val isClickable = node.isClickable
        val isEditable = node.isEditable

        val rect = Rect()
        node.getBoundsInScreen(rect)

        if (text.isNotBlank() || !desc.isNullOrBlank() || isClickable || isEditable) {
            list.add(
                ScreenElement(
                    text = text,
                    contentDescription = desc,
                    viewId = viewId,
                    isClickable = isClickable,
                    isEditable = isEditable,
                    bounds = rect
                )
            )
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            traverseNode(child, list)
        }
    }

    /**
     * Click an on-screen element matching text, content description, or view ID
     */
    fun clickElement(query: String): Pair<Boolean, String> {
        val root = rootInActiveWindow ?: return Pair(false, "স্ক্রিনের অ্যাক্সেস পাওয়া যায়নি। দয়া করে নিশ্চিত করুন Accessibility চালু আছে।")

        val targetNode = findClickableNode(root, query.trim().lowercase())
        if (targetNode != null) {
            val success = targetNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            if (success) {
                return Pair(true, "'$query' বাটনে সফলভাবে ক্লিক করা হয়েছে।")
            }
        }

        // If direct action failed, attempt gesture tap at element bounds
        val bounds = findElementBounds(root, query.trim().lowercase())
        if (bounds != null) {
            val centerX = bounds.centerX().toFloat()
            val centerY = bounds.centerY().toFloat()
            val tapped = performTapGesture(centerX, centerY)
            if (tapped) {
                return Pair(true, "'$query' এর অবস্থানে জেসচার ট্যাপ করা হয়েছে।")
            }
        }

        return Pair(false, "স্ক্রিনে '$query' নামের কোনো বাটন বা উপাদান পাওয়া যায়নি।")
    }

    private fun findClickableNode(root: AccessibilityNodeInfo, query: String): AccessibilityNodeInfo? {
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)

        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()
            val text = node.text?.toString()?.lowercase().orEmpty()
            val desc = node.contentDescription?.toString()?.lowercase().orEmpty()
            val id = node.viewIdResourceName?.lowercase().orEmpty()

            if (text.contains(query) || desc.contains(query) || id.contains(query)) {
                // Find clickable ancestor or node itself
                var cur: AccessibilityNodeInfo? = node
                while (cur != null) {
                    if (cur.isClickable) return cur
                    cur = cur.parent
                }
                return node
            }

            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { queue.add(it) }
            }
        }
        return null
    }

    private fun findElementBounds(root: AccessibilityNodeInfo, query: String): Rect? {
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)

        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()
            val text = node.text?.toString()?.lowercase().orEmpty()
            val desc = node.contentDescription?.toString()?.lowercase().orEmpty()

            if (text.contains(query) || desc.contains(query)) {
                val rect = Rect()
                node.getBoundsInScreen(rect)
                if (!rect.isEmpty) return rect
            }

            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { queue.add(it) }
            }
        }
        return null
    }

    /**
     * Type text into an editable field (or focused field)
     */
    fun enterText(fieldQuery: String?, textToEnter: String): Pair<Boolean, String> {
        val root = rootInActiveWindow ?: return Pair(false, "স্ক্রিনের অ্যাক্সেস পাওয়া যায়নি")

        val targetNode = if (!fieldQuery.isNullOrBlank()) {
            findEditableNode(root, fieldQuery.trim().lowercase())
        } else {
            root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT) ?: findAnyEditableNode(root)
        }

        if (targetNode != null) {
            val args = Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, textToEnter)
            }
            val success = targetNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
            return if (success) {
                Pair(true, "সফলভাবে '$textToEnter' টাইপ করা হয়েছে।")
            } else {
                Pair(false, "টেক্সট ফিল্ডে টাইপ করা সম্ভব হয়নি।")
            }
        }

        return Pair(false, "স্ক্রিনে কোনো টাইপ করার মতো ফিল্ড পাওয়া যায়নি।")
    }

    private fun findEditableNode(root: AccessibilityNodeInfo, query: String): AccessibilityNodeInfo? {
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)

        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()
            val text = node.text?.toString()?.lowercase().orEmpty()
            val hint = node.hintText?.toString()?.lowercase().orEmpty()

            if (node.isEditable && (text.contains(query) || hint.contains(query))) {
                return node
            }

            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { queue.add(it) }
            }
        }
        return null
    }

    private fun findAnyEditableNode(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)

        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()
            if (node.isEditable) return node
            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { queue.add(it) }
            }
        }
        return null
    }

    /**
     * Scroll active screen forward (down) or backward (up)
     */
    fun scrollScreen(forward: Boolean): Pair<Boolean, String> {
        val root = rootInActiveWindow ?: return Pair(false, "স্ক্রিন অ্যাক্সেস নেই")

        val scrollableNode = findScrollableNode(root)
        if (scrollableNode != null) {
            val action = if (forward) AccessibilityNodeInfo.ACTION_SCROLL_FORWARD else AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
            val success = scrollableNode.performAction(action)
            if (success) {
                return Pair(true, if (forward) "স্ক্রল ডাউন করা হয়েছে।" else "স্ক্রল আপ করা হয়েছে।")
            }
        }

        // Gesture swipe fallback
        val displayMetrics = resources.displayMetrics
        val width = displayMetrics.widthPixels.toFloat()
        val height = displayMetrics.heightPixels.toFloat()
        val startY = if (forward) height * 0.75f else height * 0.25f
        val endY = if (forward) height * 0.25f else height * 0.75f
        val swiped = performSwipeGesture(width / 2f, startY, width / 2f, endY)

        return if (swiped) {
            Pair(true, if (forward) "নিচে স্ক্রল করা হয়েছে।" else "উপরে স্ক্রল করা হয়েছে।")
        } else {
            Pair(false, "স্ক্রল করা সম্ভব হয়নি।")
        }
    }

    private fun findScrollableNode(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)

        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()
            if (node.isScrollable) return node
            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { queue.add(it) }
            }
        }
        return null
    }

    /**
     * Perform global device navigation (Home, Back, Recents, Notifications, Quick Settings)
     */
    fun performDeviceNavigation(actionType: String): Pair<Boolean, String> {
        val globalAction = when (actionType.uppercase()) {
            "HOME" -> GLOBAL_ACTION_HOME
            "BACK" -> GLOBAL_ACTION_BACK
            "RECENTS" -> GLOBAL_ACTION_RECENTS
            "NOTIFICATIONS" -> GLOBAL_ACTION_NOTIFICATIONS
            "QUICK_SETTINGS" -> GLOBAL_ACTION_QUICK_SETTINGS
            "LOCK_SCREEN" -> GLOBAL_ACTION_LOCK_SCREEN
            else -> return Pair(false, "অজানা সিস্টেম অ্যাকশন: $actionType")
        }

        val success = performGlobalAction(globalAction)
        val actionName = when (actionType.uppercase()) {
            "HOME" -> "হোম স্ক্রিনে ফিরে আসা হয়েছে"
            "BACK" -> "পূর্ববর্তী স্ক্রিনে যাওয়া হয়েছে"
            "RECENTS" -> "রিসেন্ট অ্যাপস খোলা হয়েছে"
            "NOTIFICATIONS" -> "নোটিফিকেশন প্যানেল নামানো হয়েছে"
            "QUICK_SETTINGS" -> "কুইক সেটিংস খোলা হয়েছে"
            "LOCK_SCREEN" -> "ডিভাইস লক করা হয়েছে"
            else -> actionType
        }
        return Pair(success, if (success) "$actionName!" else "অ্যাকশনটি কার্যকর করা সম্ভব হয়নি।")
    }

    private fun performTapGesture(x: Float, y: Float): Boolean {
        val path = Path().apply {
            moveTo(x, y)
        }
        val stroke = GestureDescription.StrokeDescription(path, 0, 50)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        return dispatchGesture(gesture, null, null)
    }

    private fun performSwipeGesture(startX: Float, startY: Float, endX: Float, endY: Float): Boolean {
        val path = Path().apply {
            moveTo(startX, startY)
            lineTo(endX, endY)
        }
        val stroke = GestureDescription.StrokeDescription(path, 0, 250)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        return dispatchGesture(gesture, null, null)
    }
}
