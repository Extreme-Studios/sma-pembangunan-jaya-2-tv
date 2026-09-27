package com.mugarutv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mugarutv.data.model.Playlist
import com.mugarutv.data.model.Song
import com.mugarutv.ui.components.SongListItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    songs: List<Song>,
    genres: List<String>,
    playlists: List<Playlist>,
    errorMessage: String?,
    isLoading: Boolean,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    canLike: Boolean = false,
    onSongClick: (Song) -> Unit,
    onArtistClick: (String) -> Unit,
    onAddSongToPlaylist: (playlistId: String, songId: String) -> Unit,
    onLoginForLike: () -> Unit = {},
    onLikeFilm: (Song) -> Unit = {}
) {
    var query by remember { mutableStateOf("") }
    var selectedGenre by remember { mutableStateOf("Semua") }
    var songToAdd by remember { mutableStateOf<Song?>(null) }
    val listState = rememberLazyListState()
    
    val filteredSongs = songs.filter { song ->
        val matchesQuery = query.isBlank() ||
            song.title.contains(query, true) ||
            song.year.contains(query, true) ||
            song.synopsis.contains(query, true) ||
            song.category.contains(query, true) || song.channelCategories.any { it.contains(query, true) }
        val matchesGenre = selectedGenre == "Semua" || song.category.equals(selectedGenre, ignoreCase = true) ||
            song.channelCategories.any { it.equals(selectedGenre, ignoreCase = true) }
        matchesQuery && matchesGenre
    }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(MaterialTheme.colorScheme.background).padding(horizontal = 20.dp, vertical = 16.dp)) {

                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Cari judul, tahun, kategori, atau deskripsi...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedBorderColor = MaterialTheme.colorScheme.primary
                    ),
                    singleLine = true
                )
                Spacer(Modifier.height(12.dp))
                ScrollableTabRow(
                    selectedTabIndex = genres.indexOf(selectedGenre).coerceAtLeast(0),
                    edgePadding = 0.dp,
                    containerColor = Color.Transparent,
                    divider = {},
                    indicator = {}
                ) {
                    genres.forEach { genre ->
                        val isSelected = selectedGenre == genre
                        Tab(
                            selected = isSelected,
                            onClick = { selectedGenre = genre },
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            SuggestionChip(
                                onClick = { selectedGenre = genre },
                                label = { Text(genre) },
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    labelColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                border = null,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                        }
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            SchoolBackground()
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize(),
                state = listState,
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (filteredSongs.isEmpty() && !isLoading) {
                    item {
                        Column(modifier = Modifier.fillMaxWidth().padding(top = 40.dp)) {
                            Text(
                                "Tidak ada live streaming ditemukan.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                
                items(filteredSongs, key = { it.id }) { song ->
                    SongListItem(
                        song = song,
                        onClick = { onSongClick(song) },
                        onArtistClick = onArtistClick,
                        onAddToPlaylist = { songToAdd = song },
                        canLike = canLike,
                        onLoginForLike = onLoginForLike,
                        onLike = onLikeFilm
                    )
                }
            }
        }
    }

    if (songToAdd != null) {
        AlertDialog(
            onDismissRequest = { songToAdd = null },
            title = { Text("Simpan ke Playlist") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (playlists.isEmpty()) {
                        Text("Belum ada playlist.")
                    } else {
                        playlists.forEach { playlist ->
                            TextButton(
                                onClick = {
                                    onAddSongToPlaylist(playlist.id, songToAdd!!.id)
                                    songToAdd = null
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(playlist.name, textAlign = androidx.compose.ui.text.style.TextAlign.Start, modifier = Modifier.fillMaxWidth())
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { songToAdd = null }) { Text("Tutup") }
            }
        )
    }
}
