package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GppMaybe
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CriticWarning
import com.example.ui.theme.*

@Composable
fun CriticInterceptorDialog(
    warning: CriticWarning,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("critic_interceptor_dialog"),
        containerColor = IrisSurfaceElevated,
        shape = RoundedCornerShape(16.dp),
        icon = {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(IrisAlertRed.copy(alpha = 0.2f))
                    .border(1.dp, IrisAlertRed, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = "Critic Shield",
                    tint = IrisAlertRed,
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "নিরাপত্তা নিশ্চিতকরণ",
                    color = IrisCyanPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "আপনার তথ্যের সুরক্ষার্থে একটি বিনীত সতর্কতা",
                    color = IrisTextMuted,
                    fontSize = 11.sp
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = warning.reason,
                    color = IrisTextPrimary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, IrisBorder, RoundedCornerShape(8.dp)),
                    color = IrisSurface
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "অনুরোধের বিবরণ:",
                            color = IrisTextMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = warning.actionName,
                            color = IrisCyanPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = "Safe Shield",
                        tint = IrisEmeraldAccent,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "আপনার স্পষ্ট অনুমোদন ছাড়া কোনো ডাটা মুছে ফেলা বা পরিবর্তন করা হবে না।",
                        color = IrisTextSecondary,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = IrisAlertRed,
                    contentColor = IrisTextPrimary
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("confirm_critic_action_button")
            ) {
                Text("হ্যাঁ, এগিয়ে যান", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = IrisTextSecondary),
                modifier = Modifier.testTag("dismiss_critic_action_button")
            ) {
                Text("না, বাতিল করুন", fontSize = 12.sp)
            }
        }
    )
}
