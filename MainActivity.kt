package com.mouad.fdt

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private val Teal = Color(0xFF42D6BD)
private val Amber = Color(0xFFF3BC63)
data class DemoEvent(val id: Long, val http: Boolean, val bytes: Int) {
    val host: String get() = if (http) "api.example.test" else "fitness.example.test"
}
data class DemoSession(val running: Boolean = false, val total: Long = 0, val bytes: Long = 0, val candidates: Long = 0, val events: List<DemoEvent> = emptyList())
class InspectorViewModel : ViewModel() {
    private val mutable = MutableStateFlow(DemoSession())
    val state = mutable.asStateFlow()
    init { viewModelScope.launch { while (true) {
        delay(1000)
        mutable.update { s ->
            if (!s.running) s else {
                val n = s.total + 1
                val e = DemoEvent(n, n % 3L == 0L, 180 + (n % 9).toInt() * 124)
                s.copy(total = n, bytes = s.bytes + e.bytes, candidates = s.candidates + if (e.http) 1 else 0, events = (listOf(e) + s.events).take(300))
            }
        }
    } } }
    fun toggle() { mutable.update { it.copy(running = !it.running) } }
    fun clear() { mutable.value = DemoSession() }
}
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(primary = Teal, secondary = Amber, background = Color(0xFF0B1020), surface = Color(0xFF151E30))) { App() }
        }
    }
}
@Composable
private fun App(vm: InspectorViewModel = viewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    var advanced by rememberSaveable { mutableStateOf(false) }
    var section by rememberSaveable { mutableIntStateOf(0) }
    var query by rememberSaveable { mutableStateOf("") }
    var httpOnly by rememberSaveable { mutableStateOf(false) }
    var packets by rememberSaveable { mutableStateOf(false) }
    var paused by remember { mutableStateOf(false) }
    var frozen by remember { mutableStateOf(DemoSession()) }
    var selected by remember { mutableStateOf<DemoEvent?>(null) }
    var delete by remember { mutableStateOf(false) }
    Scaffold(bottomBar = { NavigationBar {
        NavigationBarItem(selected = !advanced, onClick = { advanced = false }, icon = { Text("01") }, label = { Text("Protection") })
        NavigationBarItem(selected = advanced, onClick = { advanced = true }, icon = { Text("02") }, label = { Text("Advanced") })
    } }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Spacer(Modifier.height(4.dp))
            Text("FDT · Health Data Protection", style = MaterialTheme.typography.titleLarge)
            Text("DEMO — no real traffic is scanned or blocked", color = Amber, style = MaterialTheme.typography.labelMedium)
            Text(if (s.running) "Demo running · Protection is not active" else "Demo stopped · Protection is not active", style = MaterialTheme.typography.bodySmall)
            if (!advanced) {
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    Panel("Your health data. Your control.") {
                        Text("A simpler way to review potential exposure", style = MaterialTheme.typography.headlineMedium)
                        Text("Preview a future tool for spotting health data sent over unencrypted connections.")
                        Button(onClick = vm::toggle, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text(if (s.running) "Stop demo" else "Try Scan & Block demo") }
                        Text("Capture, detection and blocking are not implemented. PICOOC is a planned test case, not an observed app.", style = MaterialTheme.typography.bodySmall)
                    }
                    Panel(if (s.total == 0L) "No demo activity yet" else if (s.running) "Demo session in progress" else "Demo session results") {
                        Text("Synthetic events: ${s.total}")
                        Text("Potential exposures (fixtures): ${s.candidates}")
                        Text("Real connections blocked: 0 — unavailable")
                        if (s.total > 0L) Text("Fixtures are not evidence of a leak. Encrypted content is not inspected.")
                        TextButton(onClick = { section = 1; advanced = true }) { Text("Review demo results") }
                    }
                    Text("Future blocking may interrupt app synchronization. Review and undo controls must be implemented before enabling it.", style = MaterialTheme.typography.bodySmall)
                    OutlinedButton(onClick = { advanced = true }) { Text("Open Advanced") }
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Traffic", "Findings", "Session").forEachIndexed { i, label ->
                        FilterChip(selected = section == i, onClick = { section = i }, label = { Text(label) })
                    }
                }
                if (section == 2) {
                    Panel("Shared demo session") {
                        Text("${s.total} synthetic events · ${s.bytes / 1024} KiB generated")
                        Text("Both pages use the same session. Switching pages does not restart it. Buffers hold 300 entries; totals are session-wide. Data is in memory only.")
                        Button(onClick = vm::toggle) { Text(if (s.running) "Stop demo" else "Resume demo") }
                        OutlinedButton(onClick = { delete = true }) { Text("Delete demo session") }
                    }
                } else {
                    if (section == 0) {
                        Chart(s.events.take(60).reversed())
                        OutlinedTextField(value = query, onValueChange = { query = it }, label = { Text("Search destination") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(selected = packets, onClick = { packets = !packets }, label = { Text(if (packets) "Packets" else "HTTP messages") })
                            FilterChip(selected = httpOnly, onClick = { httpOnly = !httpOnly }, label = { Text("Without TLS") })
                        }
                        TextButton(onClick = { if (!paused) frozen = s; paused = !paused }) { Text(if (paused) "Resume view" else "Pause view (demo keeps running)") }
                    } else Text("Demo fixtures — not detector output", color = Amber)
                    val shown = if (section == 0 && paused) frozen else s
                    val rows = shown.events.filter {
                        if (section == 1) it.http else it.host.contains(query, true) && (!httpOnly || it.http)
                    }
                    LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (rows.isEmpty()) item { Text("No entries. Start the demo on Protection or adjust filters.") }
                        items(rows, key = { it.id }) { e ->
                            Card(onClick = { selected = e }, modifier = Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(if (section == 0 && packets) "Synthetic packet #${e.id} · ${e.bytes} B" else "Synthetic message #${e.id}")
                                    Text(e.host, style = MaterialTheme.typography.titleMedium)
                                    Text(if (e.http) "HTTP without TLS · weight-field fixture" else "HTTPS · content not inspected", color = if (e.http) Amber else Teal)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    selected?.let { e ->
        var reveal by remember(e.id) { mutableStateOf(false) }
        AlertDialog(onDismissRequest = { selected = null }, title = { Text("Demo evidence #${e.id}") }, text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(e.host)
                Text(if (e.http) "HTTP without TLS · synthetic POST /measurements" else "HTTPS · payload unavailable")
                if (e.http) {
                    Text("Weight-related field: body_weight")
                    Text(if (reveal) "72.4 (fictional value)" else "Value: hidden")
                    TextButton(onClick = { reveal = !reveal }) { Text(if (reveal) "Hide value" else "Reveal sample value") }
                }
                Text("Source: demo fixture. Nothing was captured or blocked. No claim is made about PICOOC.")
            }
        }, confirmButton = { TextButton(onClick = { selected = null }) { Text("Close") } })
    }
    if (delete) AlertDialog(onDismissRequest = { delete = false }, title = { Text("Delete demo session?") }, text = { Text("This stops the demo and clears all in-memory entries.") }, confirmButton = { TextButton(onClick = { vm.clear(); frozen = DemoSession(); paused = false; selected = null; delete = false }) { Text("Delete") } }, dismissButton = { TextButton(onClick = { delete = false }) { Text("Cancel") } })
}
@Composable
private fun Panel(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(title, color = Teal, style = MaterialTheme.typography.titleMedium)
        content()
    } }
}
@Composable
private fun Chart(events: List<DemoEvent>) {
    Text("Synthetic traffic · bytes/event · latest 60 events", style = MaterialTheme.typography.labelSmall)
    val max = events.maxOfOrNull { it.bytes }?.coerceAtLeast(1) ?: 1
    Canvas(Modifier.fillMaxWidth().height(80.dp)) {
        drawLine(Color.Gray, Offset(0f, size.height), Offset(size.width, size.height))
        for (i in 1 until events.size) {
            fun point(n: Int) = Offset(size.width * n / 59f, size.height * (1f - events[n].bytes.toFloat() / max))
            drawLine(Teal, point(i - 1), point(i), 3f)
        }
    }
}
