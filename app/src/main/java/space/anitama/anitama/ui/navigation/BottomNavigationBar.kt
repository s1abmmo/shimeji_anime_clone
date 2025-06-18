package space.anitama.anitama.ui.navigation

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import space.anitama.anitama.R

@Composable
fun BottomNavigationBar(navController: NavController, currentDestination: String) {
    NavigationBar(
        containerColor = Color.White,
        modifier = Modifier
            .shadow(8.dp, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
    ) {
        NavigationBarItem(
            icon = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_overlay),
                    contentDescription = "Overlay",
                    modifier = Modifier.size(32.dp)
                )
            },
            label = { Text("Overlay") },
            selected = currentDestination == "overlay",
            onClick = { navController.navigate("overlay") { popUpTo(navController.graph.startDestinationId) } }
        )
        NavigationBarItem(
            icon = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_settings),
                    contentDescription = "Settings",
                    modifier = Modifier.size(32.dp)
                )
            },
            label = { Text("Settings") },
            selected = currentDestination == "settings",
            onClick = { navController.navigate("settings") { popUpTo(navController.graph.startDestinationId) } }
        )
    }
}