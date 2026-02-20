package com.martin.storage.ui.theme

import android.content.Intent
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.martin.storage.EDGEPADDING
import com.martin.storage.MainActivity
import com.martin.storage.R
import com.martin.storage.SettingActivity
import com.martin.storage.StorageActivity
import com.martin.storage.TOPPADDING

@Composable
fun BottomNavigation(modifier: Modifier = Modifier, activeTab: Int) {
    var activeBottomTab by remember { mutableIntStateOf(activeTab) }
    val context = LocalContext.current
    val bottomTabs = listOf(
        { context.startActivity(Intent(context, MainActivity::class.java)) },
        { context.startActivity(Intent(context, StorageActivity::class.java)) },
        { context.startActivity(Intent(context, SettingActivity::class.java)) }
    )
    val tabIcons = listOf(R.drawable.homeicon, R.drawable.storagelist, R.drawable.options)

    NavigationBar(modifier = Modifier.height(60.dp)) {
        bottomTabs.forEachIndexed { index, item ->
            NavigationBarItem(
                icon = {
                    Icon(
                        painter = painterResource(tabIcons[index]),
                        contentDescription = "Bottom navigation icon",
                    )
                },
                selected = activeBottomTab == index,
                onClick = {
                    activeBottomTab = index
                    item()
                }
            )
        }
    }
}

@Composable
fun TopTitle(modifier: Modifier = Modifier, text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .padding(start = EDGEPADDING.dp, top = TOPPADDING.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = text,
            fontSize = 20.sp,
            modifier = Modifier.padding(0.dp),
            fontWeight = FontWeight.Bold,
            style = TextStyle(
                platformStyle = PlatformTextStyle(
                    includeFontPadding = false,
                ),
            ),
        )
    }
}