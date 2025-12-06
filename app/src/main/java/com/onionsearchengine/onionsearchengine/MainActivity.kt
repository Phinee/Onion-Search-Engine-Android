package com.onionsearchengine.onionsearchengine 

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.onionsearchengine.onionsearchengine.ui.theme.OnionSearchEngineTheme
import androidx.compose.foundation.lazy.rememberLazyListState
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter


class MainActivity : ComponentActivity() {
    private val searchViewModel: SearchViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            OnionSearchEngineTheme {
                val appState by searchViewModel.appState.collectAsState()

                when (appState) {
                    is AppState.Initializing -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator()
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("First time setup, please wait...")
                            }
                        }
                    }
                    is AppState.Ready -> {
                        OnionSearchApp(viewModel = searchViewModel)
                    }
                    is AppState.Error -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Error: ${(appState as AppState.Error).message}")
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun OnionSearchApp(viewModel: SearchViewModel) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "search") {
        composable("search") {
            SearchScreen(navController = navController)
        }

        composable("results/{query}") { backStackEntry ->
            val query = backStackEntry.arguments?.getString("query") ?: ""
            ResultsScreen(viewModel = viewModel, query = query, navController = navController)
        }

    }
}

@Composable
fun SearchScreen(navController: NavController) { 
    var text by remember { mutableStateOf("") }

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.onionlogo),
                contentDescription = "Onion Search Engine Logo",
                modifier = Modifier.size(120.dp).padding(bottom = 16.dp)
            )
            Text("Onion Search Engine", fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(32.dp))
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Search .onion sites...") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = {
                if (text.isNotBlank()) {
                    navController.navigate("results/$text")
                }
            }) {
                Text("Search")
            }
            Spacer(Modifier.weight(1f))
            }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultsScreen(viewModel: SearchViewModel, query: String, navController: NavController) {
    val results by viewModel.results.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    val context = LocalContext.current
    val listState = rememberLazyListState()

    LaunchedEffect(key1 = query) {
        viewModel.newSearch(query)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Results: \"$query\"", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { navController.navigate("search") { popUpTo("search") { inclusive = true } } }) {
                        Icon(imageVector = Icons.Default.Search, contentDescription = "New Search")
                    }
                }
            )
        }
    )



    { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            if (results.isEmpty() && isLoading) {
                CircularProgressIndicator()
            } else if (error != null) {
                Text(text = error!!)
            } else {
                LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                    items(results) { result ->
                        ResultItem(result = result, onClick = { url ->
                            val fullUrl = if (!url.startsWith("http")) "http://$url" else url
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(fullUrl))
                                context.startActivity(intent)
                            } catch (e: ActivityNotFoundException) {
                                Toast.makeText(context, "Tor Browser or a compatible app is required.", Toast.LENGTH_LONG).show()
                            }
                        })
                    }
                    if (isLoading) {
                        item {
                            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.Center) {
                                CircularProgressIndicator()
                            }
                        }
                    }
                }
            }
        }


    }

    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .collect { lastVisibleItemIndex ->
                if (lastVisibleItemIndex != null) {
                    val totalItems = listState.layoutInfo.totalItemsCount
                    if (totalItems > 0 && lastVisibleItemIndex >= totalItems - 5) {
                        viewModel.loadNextPage()
                    }
                }
            }
    }

}

@Composable
fun ResultItem(result: SearchResult, onClick: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { result.url?.let { onClick(it) } }
            .padding(16.dp)
    ) {
        Text(text = result.title ?: "No Title", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = result.url ?: "", fontSize = 14.sp, color = MaterialTheme.colorScheme.primary, textDecoration = TextDecoration.Underline, modifier = Modifier.clickable { result.url?.let { onClick(it) } })
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = result.snippet ?: "", fontSize = 14.sp, lineHeight = 20.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
    }
}