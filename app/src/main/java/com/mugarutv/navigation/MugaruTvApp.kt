package com.mugarutv.navigation

import android.app.Activity
import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.mugarutv.data.local.PortalSessionStore
import com.mugarutv.data.local.PlaylistLocalStore
import com.mugarutv.data.model.PortalPost
import com.mugarutv.data.model.Song
import com.mugarutv.data.repository.ApiSongRepository
import com.mugarutv.data.repository.PortalRepository
import com.mugarutv.data.repository.PlaylistRepository
import com.mugarutv.data.repository.SongRegistrationRepository
import com.mugarutv.data.repository.SCHOOL_APPS_SCRIPT_ENDPOINT
import com.mugarutv.ui.screens.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

sealed class AppScreen(val route: String, val label: String, val icon: ImageVector) {
    data object Home : AppScreen("home", "Beranda", Icons.Default.Home)
    data object Library : AppScreen("library", "Koleksi", Icons.AutoMirrored.Filled.List)
    data object Live : AppScreen("live", "Live", Icons.Default.PlayArrow)
    data object PortalKita : AppScreen("portal_kita", "Board Info", Icons.Default.Info)
    data object About : AppScreen("about", "Profil", Icons.Default.AccountCircle)
}

@Composable
fun MugaruTvApp(repository: ApiSongRepository = remember { ApiSongRepository() }) {
    val context = LocalContext.current
    val activity = context as? Activity
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val playlistRepository = remember { PlaylistRepository(PlaylistLocalStore(context)) }
    val portalRepository = remember { PortalRepository() }
    val registrationRepository = remember { SongRegistrationRepository() }
    val portalSessionStore = remember { PortalSessionStore(context) }
    val portalSession by portalSessionStore.sessionFlow.collectAsState(initial = null)

    val tabs = if (SCHOOL_APPS_SCRIPT_ENDPOINT.isBlank()) {
        listOf(AppScreen.Home, AppScreen.Library, AppScreen.Live, AppScreen.About)
    } else {
        listOf(AppScreen.Home, AppScreen.Library, AppScreen.Live, AppScreen.PortalKita, AppScreen.About)
    }
    var currentTab by remember { mutableStateOf<AppScreen>(AppScreen.Home) }
    val tabHistory = remember { mutableStateListOf<AppScreen>().apply { add(AppScreen.Home) } }
    var selectedArtist by remember { mutableStateOf<String?>(null) }
    var selectedAlbum by remember { mutableStateOf<Pair<String, String>?>(null) }
    var selectedMovie by remember { mutableStateOf<Song?>(null) }
    var playingMovie by remember { mutableStateOf<Song?>(null) }
    var selectedPortalPost by remember { mutableStateOf<PortalPost?>(null) }
    var showPortalSubmission by remember { mutableStateOf(false) }
    var showRegistrationFromAbout by remember { mutableStateOf(false) }
    var uploadFormMode by remember { mutableStateOf(UploadFormMode.Video) }
    var returnToLibraryAfterLogin by remember { mutableStateOf(false) }
    var songs by remember { mutableStateOf(repository.getFallbackSongs()) }
    var isLoadingSongs by remember { mutableStateOf(false) }
    var isRefreshingSongs by remember { mutableStateOf(false) }
    var songsError by remember { mutableStateOf<String?>(null) }
    val genres = remember(songs) { repository.getGenres(songs) }
    val artists = remember(songs) { repository.getArtists(songs) }
    val playlists by playlistRepository.playlists.collectAsState(initial = emptyList())
    var portalPosts by remember { mutableStateOf<List<PortalPost>>(emptyList()) }
    var isLoadingPortal by remember { mutableStateOf(true) }
    var portalError by remember { mutableStateOf<String?>(null) }
    val portalCategories = remember { portalRepository.getCategories() }
    val configuration = LocalConfiguration.current
    val isPlayerLandscapeFullscreen = playingMovie != null && configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    fun navigateTo(screen: AppScreen) {
        selectedArtist = null
        selectedAlbum = null
        selectedMovie = null
        playingMovie = null
        selectedPortalPost = null
        showPortalSubmission = false
        showRegistrationFromAbout = false
        if (currentTab != screen) {
            currentTab = screen
            tabHistory.add(screen)
        }
    }

    fun refreshSongs(showBlocking: Boolean) {
        if (isRefreshingSongs) return
        scope.launch {
            if (showBlocking) isLoadingSongs = true else isRefreshingSongs = true
            val activeLikeUserId = portalSession?.username
            repository.fetchSongs(activeLikeUserId, forceRefresh = true).onSuccess { apiSongs ->
                songs = apiSongs
                songsError = null
            }.onFailure { err ->
                songsError = err.message ?: "Gagal memuat data."
            }
            isLoadingSongs = false
            isRefreshingSongs = false
        }
    }

    fun openSong(song: Song) {
        selectedMovie = null
        playingMovie = song
        scope.launch {
            repository.updateFilmView(song.id, song.youtubeUrl.ifBlank { song.movieUrl })
            refreshSongs(false)
        }
    }

    fun refreshPortalPosts(showBlocking: Boolean = true) {
        scope.launch {
            if (showBlocking) isLoadingPortal = true
            portalRepository.fetchActivePosts().onSuccess { posts ->
                portalPosts = posts
                portalError = null
            }.onFailure { error ->
                portalError = error.message ?: "Gagal memuat Board Info."
            }
            isLoadingPortal = false
        }
    }

    LaunchedEffect(Unit) {
        refreshSongs(showBlocking = false)
        refreshPortalPosts(showBlocking = true)

    }

    BackHandler {
        when {
            playingMovie != null -> playingMovie = null
            selectedMovie != null -> selectedMovie = null
            showPortalSubmission -> showPortalSubmission = false
            showRegistrationFromAbout -> showRegistrationFromAbout = false
            selectedPortalPost != null -> selectedPortalPost = null
            selectedAlbum != null -> selectedAlbum = null
            selectedArtist != null -> selectedArtist = null
            tabHistory.size > 1 -> {
                tabHistory.removeAt(tabHistory.lastIndex)
                currentTab = tabHistory.last()
            }
            else -> activity?.moveTaskToBack(true)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            bottomBar = {
                if (!isPlayerLandscapeFullscreen) {
                    Column {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 4.dp
                    ) {
                        tabs.forEach { screen ->
                            NavigationBarItem(
                                selected = currentTab == screen,
                                onClick = { navigateTo(screen) },
                                icon = { Icon(screen.icon, contentDescription = screen.label) },
                                label = { Text(screen.label, fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    unselectedIconColor = MaterialTheme.colorScheme.outline,
                                    unselectedTextColor = MaterialTheme.colorScheme.outline,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                )
                            )
                        }
                    }
                    }
                }
            }
        ) { innerPadding ->
            val modifier = if (isPlayerLandscapeFullscreen) Modifier else Modifier.padding(innerPadding)

            when {
            playingMovie != null -> {
                val movie = playingMovie!!
                VideoPlayerScreen(
                    title = movie.title,
                    rawUrl = movie.youtubeUrl.ifBlank { movie.movieUrl },
                    year = movie.year,
                    description = movie.synopsis.ifBlank { movie.description },
                    modifier = modifier,
                    onBack = { playingMovie = null }
                )
            }
            selectedMovie != null -> {
                MovieDetailScreen(
                    movie = selectedMovie!!,
                    modifier = modifier,
                    canEditMetadata = portalSession?.status.equals("ACTIVE", ignoreCase = true),
                    onWatchFilm = { url ->
                        playingMovie = selectedMovie
                        scope.launch {
                            repository.updateFilmView(selectedMovie!!.id, url)
                            refreshSongs(false)
                        }
                    },
                    onSaveMetadata = { title, year, deskripsi ->
                        val session = portalSession
                        if (session == null) {
                            Result.failure(IllegalStateException("Login admin diperlukan."))
                        } else {
                            repository.updateFilmMetadata(
                                filmId = selectedMovie!!.id,
                                username = session.username,
                                title = title,
                                year = year,
                                deskripsi = deskripsi
                            )
                        }
                    },
                    onMetadataSaved = { title, year, deskripsi ->
                        selectedMovie = selectedMovie?.copy(
                            title = title,
                            year = year,
                            synopsis = deskripsi,
                            description = deskripsi,
                            releaseYear = year
                        )
                        refreshSongs(false)
                    },
                    onDeleteFilm = {
                        val session = portalSession
                        if (session == null) {
                            Result.failure(IllegalStateException("Login admin diperlukan."))
                        } else {
                            repository.deleteFilm(
                                filmId = selectedMovie!!.id,
                                username = session.username,
                                videoId = selectedMovie!!.videoId,
                                videoUrl = selectedMovie!!.youtubeUrl.ifBlank { selectedMovie!!.movieUrl }
                            )
                        }
                    },
                    onFilmDeleted = {
                        selectedMovie = null
                        refreshSongs(false)
                    }
                )
            }
            showPortalSubmission -> {
                PortalPosterSubmissionScreen(
                    modifier = modifier,
                    onBack = { showPortalSubmission = false },
                    onSubmitted = { showPortalSubmission = false; refreshPortalPosts(true) }
                )
            }
            showRegistrationFromAbout -> {
                SubmitSongScreen(modifier = modifier, mode = uploadFormMode, onBack = { showRegistrationFromAbout = false }, onSubmitted = { refreshSongs(false) })
            }
            selectedPortalPost != null -> {
                PortalPostDetailScreen(post = selectedPortalPost!!, modifier = modifier, onBack = { selectedPortalPost = null })
            }
            selectedAlbum != null -> {
                AlbumDetailScreen(
                    artistName = selectedAlbum!!.first,
                    albumName = selectedAlbum!!.second,
                    songs = songs,
                    modifier = modifier,
                    onBack = { selectedAlbum = null },
                    onSongClick = { openSong(it) }
                )
            }
            selectedArtist != null -> {
                ArtistDetailScreen(
                    artistName = selectedArtist!!,
                    songs = songs,
                    errorMessage = songsError,
                    modifier = modifier,
                    onBack = { selectedArtist = null },
                    onSongClick = { openSong(it) },
                    onAlbumClick = { selectedAlbum = selectedArtist!! to it }
                )
            }
            else -> when (currentTab) {
                AppScreen.Home -> HomeScreen(
                    songs = songs,
                    artists = artists,
                    errorMessage = songsError,
                    isLoading = isLoadingSongs,
                    isRefreshing = isRefreshingSongs,
                    onRefresh = { refreshSongs(false) },
                    modifier = modifier,
                    onSongClick = { openSong(it) },
                    onArtistClick = { selectedArtist = it }
                )
                AppScreen.Library -> LibraryScreen(
                    songs = songs,
                    genres = genres,
                    playlists = playlists,
                    errorMessage = songsError,
                    isLoading = isLoadingSongs,
                    isRefreshing = isRefreshingSongs,
                    onRefresh = { refreshSongs(false) },
                    modifier = modifier,
                    onSongClick = { openSong(it) },
                    onArtistClick = { selectedArtist = it },
                    onAddSongToPlaylist = { pId, sId -> scope.launch { playlistRepository.addSongToPlaylist(pId, sId) } },
                    canLike = portalSession?.status.equals("ACTIVE", ignoreCase = true),
                    onLoginForLike = { returnToLibraryAfterLogin = true; navigateTo(AppScreen.About) },
                    onLikeFilm = { song ->
                        val session = portalSession
                        if (session?.status?.equals("ACTIVE", ignoreCase = true) == true) {
                            scope.launch {
                                repository.updateFilmLike(song.id, session.username)
                                refreshSongs(false)
                            }
                        } else {
                            returnToLibraryAfterLogin = true
                            navigateTo(AppScreen.About)
                        }
                    }
                )
                AppScreen.Live -> LiveStreamListScreen(
                    songs = songs,
                    modifier = modifier,
                    isLoading = isLoadingSongs,
                    errorMessage = songsError,
                    onRefresh = { refreshSongs(false) },
                    onOpenLive = { song -> playingMovie = song },
                    onEditLive = { song ->
                        selectedMovie = song
                    },
                    canEdit = portalSession?.status.equals("ACTIVE", ignoreCase = true),
                    adminUsername = portalSession?.username.orEmpty()
                )
                AppScreen.PortalKita -> PortalKitaScreen(
                    posts = portalPosts,
                    categories = portalCategories,
                    isLoading = isLoadingPortal,
                    errorMessage = portalError,
                    modifier = modifier,
                    onRefresh = { refreshPortalPosts(true) },
                    onPostPreview = { selectedPortalPost = it },
                    onOpenPosterSubmission = { showPortalSubmission = true }
                )
                AppScreen.About -> AboutScreen(
                    modifier = modifier,
                    openLoginOnStart = returnToLibraryAfterLogin,
                    onLoginSuccess = {
                        if (returnToLibraryAfterLogin) {
                            returnToLibraryAfterLogin = false
                            currentTab = AppScreen.Library
                            refreshSongs(false)
                        }
                    },
                    onOpenRegistration = { uploadFormMode = UploadFormMode.Video; showRegistrationFromAbout = true },
                    onOpenChannelUpload = { uploadFormMode = UploadFormMode.Channel; showRegistrationFromAbout = true }
                )
            }
            }
        }

    }
}

