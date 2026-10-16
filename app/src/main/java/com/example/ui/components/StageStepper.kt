package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.StagesMap
import com.example.ui.theme.Primary700
import com.example.ui.theme.StageColorMap
import com.example.ui.theme.SurfacePure

val OrderedStageKeys = listOf(
    "registration" to "1. التسجيل والمناداة",
    "exam" to "2. انعقاد الامتحان بالقاعة",
    "scoring" to "3. تصحيح الأسئلة ورصد الدرجات",
    "aggregation" to "4. تجميع ومطابقة الدرجات",
    "review" to "5. المراجعة والتدقيق النهائي",
    "sheet" to "6. تفريغ وكتابة الكشف المعتمد",
    "accreditation" to "7. الاعتمادية وتوقيع المشرف",
    "announcement" to "8. إعلان النتيجة للأوائل",
    "ceremony" to "9. إقامة الحفل والتكريم وتوزيع الجوائز"
)

@Composable
fun InteractiveStageStepper(
    stages: StagesMap,
    onToggleStage: (stageKey: String, currentStatus: Boolean) -> Unit,
    modifier: Modifier = Modifier,
    /** Whether the current user may change this stage (server rules). */
    canEditStage: (stageKey: String) -> Boolean = { true }
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = SurfacePure,
        tonalElevation = 2.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "مسار المراحل التسع للجنة",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Primary700
                )
                Text(
                    text = "${stages.getCompletedCount()} من 9 مكتملة (${(stages.progressFraction * 100).toInt()}%)",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF0F766E),
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            OrderedStageKeys.forEachIndexed { index, (key, title) ->
                val stageItem = stages.getStage(key)
                val stageSet = StageColorMap[key]

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (stageItem.completed) stageSet?.bg ?: Color(0xFFF0FDF4) else Color(0xFFFAFAFA))
                        .border(
                            1.dp,
                            if (stageItem.completed) stageSet?.border ?: Color(0xFFBBF7D0) else Color(0xFFEEEEEE),
                            RoundedCornerShape(12.dp)
                        )
                        .clickable(enabled = canEditStage(key)) {
                            onToggleStage(key, stageItem.completed)
                        }
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                        .testTag("stepper_item_$key"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Checkbox / Number circle
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(
                                if (stageItem.completed) stageSet?.dot ?: Primary700 else Color(0xFFE2E8F0)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (stageItem.completed) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "مكتمل",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        } else {
                            Text(
                                text = "${index + 1}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF64748B)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = stageIcon(key),
                                contentDescription = null,
                                tint = if (stageItem.completed) stageSet?.dot ?: Color(0xFF64748B) else Color(0xFF94A3B8),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = title,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (stageItem.completed) FontWeight.Bold else FontWeight.Medium,
                                color = if (stageItem.completed) stageSet?.text ?: Color(0xFF0F172A) else Color(0xFF475569)
                            )
                        }

                        if (stageItem.completed && !stageItem.completedAt.isNullOrBlank()) {
                            Text(
                                text = "تم الإنجاز في: ${stageItem.completedAt}",
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 12.sp,
                                color = stageSet?.text?.copy(alpha = 0.8f) ?: Color(0xFF64748B)
                            )
                        }
                    }

                    // Action label
                    if (canEditStage(key)) {
                        Text(
                            text = if (stageItem.completed) "مكتمل ✓" else "تحديد كمنجز",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (stageItem.completed) stageSet?.text ?: Primary700 else Color(0xFF94A3B8)
                        )
                    }
                }
            }
        }
    }
}
