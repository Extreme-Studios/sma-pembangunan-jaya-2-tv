const SPREADSHEET_ID = "1bB0FVJIzffbhHIAXW7LspDdR1XNvKCPhUbnyC2UYBIs";

const SHEET_ADMIN = "admin";
const SHEET_FILMS = "video";
const SHEET_PORTAL = "poster_posts";
const SHEET_PORTAL_REPORTS = "portal_reports";
const PORTAL_POST_EXPIRE_HOURS = 3;
const ACTIVE_LIVE_STREAM_ID = "active_live_stream";

const FILM_COVER_FOLDER_ID = "1sqBLSCOZw6fnk2EdMAm4wBzw23opaVJf";
const FILM_VIDEO_FOLDER_ID = "1XV1h-CDoBH3hB3RSTAVxA2K2ttjbA2eq";
const PORTAL_POSTER_FOLDER_ID = "1zFKrk3ShC7OrDIQ4jEBWCxxKLXgYuHm2";

const ADMIN_HEADERS = [
  "username",
  "password"
];

const FILM_HEADERS = [
  "id",
  "title",
  "year",
  "deskripsi",
  "kategori",
  "video_url",
  "video_id",
  "thumbnail_url",
  "created_at",
  "status",
  "views",
  "likes"
];

const PORTAL_HEADERS = [
  "id",
  "user_id",
  "category",
  "poster_url",
  "posting_at",
  "status"
];

const PORTAL_REPORT_HEADERS = [
  "id",
  "post_id",
  "post_title",
  "category",
  "report_reason",
  "reported_link",
  "poster_url",
  "status"
];

function setupSchoolTvSheets() {
  const ss = SpreadsheetApp.openById(SPREADSHEET_ID);
  const adminSheet = ensureAdminSheetWithDefaults_(ss);
  const filmsSheet = ensureSheetWithHeaders_(ss, SHEET_FILMS, FILM_HEADERS);
  const portalSheet = ensureSheetWithHeaders_(ss, SHEET_PORTAL, PORTAL_HEADERS);
  const reportsSheet = ensureSheetWithHeaders_(ss, SHEET_PORTAL_REPORTS, PORTAL_REPORT_HEADERS);

  return {
    success: true,
    message: "SMA PJ2 TV sheets siap.",
    sheets: [adminSheet.getName(), filmsSheet.getName(), portalSheet.getName(), reportsSheet.getName()]
  };
}

function doGet(e) {
  try {
    setupSchoolTvSheets();
    const type = String((e && e.parameter && e.parameter.type) || "").trim();

    if (type === "health") {
      return jsonOutput_(healthCheck_());
    }

    if (type === "setup") {
      return jsonOutput_(setupSchoolTvSheets());
    }

    if (type === "portal_posts") {
      return jsonOutput_({
        success: true,
        portal_posts: getActivePortalPosts_()
      });
    }

    if (type === "youtube_meta") {
      const youtubeUrl = String((e && e.parameter && e.parameter.url) || "").trim();
      return jsonOutput_(fetchYoutubeMetadata_(youtubeUrl));
    }

    const likeUserId = String((e && e.parameter && (e.parameter.user_id || e.parameter.like_user_id)) || "").trim();
    const movies = getApprovedMovies_(likeUserId);
    const categories = getApprovedCategories_(movies);
    return jsonOutput_({
      success: true,
      movies: movies,
      films: movies,
      categories: categories
    });
  } catch (error) {
    return jsonOutput_({
      success: false,
      message: error && error.message ? error.message : "Unhandled server error (GET)."
    });
  }
}

function healthCheck_() {
  const ss = SpreadsheetApp.openById(SPREADSHEET_ID);
  const adminSheet = ensureAdminSheetWithDefaults_(ss);
  const filmsSheet = ensureSheetWithHeaders_(ss, SHEET_FILMS, FILM_HEADERS);
  const portalSheet = ensureSheetWithHeaders_(ss, SHEET_PORTAL, PORTAL_HEADERS);
  const reportsSheet = ensureSheetWithHeaders_(ss, SHEET_PORTAL_REPORTS, PORTAL_REPORT_HEADERS);

  const filmCoverFolder = DriveApp.getFolderById(FILM_COVER_FOLDER_ID);
  const portalFolder = DriveApp.getFolderById(PORTAL_POSTER_FOLDER_ID);

  const probe = uploadHealthProbe_(portalFolder);

  return {
    success: true,
    message: "Health check OK",
    spreadsheet: {
      id: SPREADSHEET_ID,
      name: ss.getName(),
      sheets: [
        adminSheet.getName(),
        filmsSheet.getName(),
        portalSheet.getName(),
        reportsSheet.getName()
      ]
    },
    drive: {
      film_cover_folder_id: FILM_COVER_FOLDER_ID,
      film_cover_folder_name: filmCoverFolder.getName(),
      portal_folder_id: PORTAL_POSTER_FOLDER_ID,
      portal_folder_name: portalFolder.getName()
    },
    upload_probe: probe
  };
}

function uploadHealthProbe_(folder) {
  const fileName = "health_probe_" + Date.now() + ".txt";
  const blob = Utilities.newBlob(
    "SMA PJ2 TV HEALTH CHECK " + new Date().toISOString(),
    "text/plain",
    fileName
  );
  const file = folder.createFile(blob);
  file.setSharing(DriveApp.Access.ANYONE_WITH_LINK, DriveApp.Permission.VIEW);
  const viewUrl = "https://drive.google.com/uc?export=view&id=" + file.getId();
  return {
    file_id: file.getId(),
    file_name: file.getName(),
    view_url: viewUrl
  };
}

function doPost(e) {
  try {
    setupSchoolTvSheets();
    const payload = JSON.parse((e && e.postData && e.postData.contents) || "{}");
    const type = String(payload.type || "").trim();

    if (type === "admin_login" || type === "hub_login") {
      return handleAdminLogin_(payload);
    }
    if (type === "admin_check_status" || type === "hub_check_status") {
      return handleAdminCheckStatus_(payload);
    }
    if (type === "admin_update_credentials") {
      return handleAdminUpdateCredentials_(payload);
    }
    if (type === "portal_post") {
      return handlePortalPost_(payload);
    }
    if (type === "portal_report") {
      return handlePortalReport_(payload);
    }
    if (type === "portal_delete") {
      return handlePortalDelete_(payload);
    }
    if (type === "content_report") {
      return handleContentReport_(payload);
    }
    if (type === "film_like") {
      return handleFilmLike_(payload);
    }
    if (type === "film_view") {
      return handleFilmView_(payload);
    }
    if (type === "film_update") {
      return handleFilmUpdate_(payload);
    }
    if (type === "film_delete") {
      return handleFilmDelete_(payload);
    }
    if (type === "channel_import" || type === "youtube_channel_import") {
      return handleYoutubeChannelImport_(payload);
    }
    if (type === "live_stream_update" || type === "active_live_update") {
      return handleLiveStreamUpdate_(payload);
    }
    if (type === "film_registration" || type === "song_registration" || type === "live_registration") {
      return handleFilmRegistration_(payload);
    }

    return jsonOutput_({ success: false, message: "Tipe request tidak dikenal." });
  } catch (error) {
    return jsonOutput_({
      success: false,
      message: error && error.message ? error.message : "Unhandled server error."
    });
  }
}

function handleMemberRegister_(payload) {
  return handleAdminLogin_(payload);
}

function handleMemberLogin_(payload) {
  return handleAdminLogin_(payload);
}

function handleMemberCheckStatus_(payload) {
  return handleAdminCheckStatus_(payload);
}

function handleYoutubeChannelImport_(payload) {
  const ss = SpreadsheetApp.openById(SPREADSHEET_ID);
  const sheet = ensureSheetWithHeaders_(ss, SHEET_FILMS, FILM_HEADERS);
  const channelUrl = normalizeYoutubeChannelUrl_(payload.channel_url || payload.channelUrl || payload.url || "");
  const maxItems = Math.min(Math.max(Number(payload.max_items || payload.maxItems || 500), 1), 1000);

  if (!channelUrl) {
    return jsonOutput_({ success: false, message: "Link channel YouTube wajib diisi." });
  }

  const channelInfo = resolveYoutubeChannelInfo_(channelUrl);
  if (!channelInfo.channelId) {
    return jsonOutput_({ success: false, message: "Channel YouTube tidak ditemukan atau tidak publik." });
  }

  const channelId = channelInfo.channelId;
  let imported = 0;
  let skipped = 0;
  const importedIds = [];

  function onImportSuccess() {
    imported++;
  }
  function onImportSkip() {
    skipped++;
  }

  // 1. Home tab (RSS + Home page)
  try {
    const rssVideos = fetchYoutubeChannelRssVideos_(channelId);
    importItemsWithCategory_(sheet, rssVideos, "Home", importedIds, onImportSuccess, onImportSkip);
  } catch (err) {
    Logger.log("Gagal import RSS Home: " + err.message);
  }
  try {
    const homeHtml = fetchText_("https://www.youtube.com/channel/" + encodeURIComponent(channelId), "GET", null);
    const homeRendererVideos = parseYoutubeVideoRendererItems_(homeHtml);
    importItemsWithCategory_(sheet, homeRendererVideos, "Home", importedIds, onImportSuccess, onImportSkip);
  } catch (err) {
    Logger.log("Gagal import Home HTML: " + err.message);
  }

  // 2. Videos tab
  try {
    const videosHtml = fetchText_("https://www.youtube.com/channel/" + encodeURIComponent(channelId) + "/videos", "GET", null);
    const videosItems = parseYoutubeVideoRendererItems_(videosHtml);
    importItemsWithCategory_(sheet, videosItems, "Videos", importedIds, onImportSuccess, onImportSkip);
  } catch (err) {
    Logger.log("Gagal import Videos: " + err.message);
  }

  // 3. Live tab
  try {
    const liveHtml = fetchText_("https://www.youtube.com/channel/" + encodeURIComponent(channelId) + "/streams", "GET", null);
    const liveItems = parseYoutubeVideoRendererItems_(liveHtml);
    importItemsWithCategory_(sheet, liveItems, "Live", importedIds, onImportSuccess, onImportSkip);
  } catch (err) {
    Logger.log("Gagal import Live: " + err.message);
  }

  // 4. Podcast tab
  try {
    const podcastHtml = fetchText_("https://www.youtube.com/channel/" + encodeURIComponent(channelId) + "/podcasts", "GET", null);
    const podcastItems = parseYoutubeVideoRendererItems_(podcastHtml);
    importItemsWithCategory_(sheet, podcastItems, "Podcast", importedIds, onImportSuccess, onImportSkip);
  } catch (err) {
    Logger.log("Gagal import Podcast: " + err.message);
  }

  // 5. Playlist tab
  try {
    const playlistsHtml = fetchText_("https://www.youtube.com/channel/" + encodeURIComponent(channelId) + "/playlists", "GET", null);
    const playlistIds = parseYoutubePlaylists_(playlistsHtml);
    const maxPlaylists = Math.min(playlistIds.length, 5);
    for (let i = 0; i < maxPlaylists; i++) {
      try {
        const plHtml = fetchText_("https://www.youtube.com/playlist?list=" + encodeURIComponent(playlistIds[i]), "GET", null);
        const plItems = parseYoutubeVideoRendererItems_(plHtml);
        importItemsWithCategory_(sheet, plItems, "Playlist", importedIds, onImportSuccess, onImportSkip);
      } catch (e) {
        Logger.log("Gagal import playlist " + playlistIds[i] + ": " + e.message);
      }
    }
  } catch (err) {
    Logger.log("Gagal import Playlists: " + err.message);
  }

  // 6. Post tab
  try {
    const communityHtml = fetchText_("https://www.youtube.com/channel/" + encodeURIComponent(channelId) + "/community", "GET", null);
    const communityPosts = parseYoutubeCommunityPosts_(communityHtml);
    importItemsWithCategory_(sheet, communityPosts, "Post", importedIds, onImportSuccess, onImportSkip);
  } catch (err) {
    Logger.log("Gagal import Posts: " + err.message);
  }

  return jsonOutput_({
    success: true,
    message: "Import channel selesai.",
    total: imported + skipped,
    imported: imported,
    skipped: skipped,
    channel_id: channelId,
    channel_title: channelInfo.title || "",
    imported_video_ids: importedIds
  });
}

function importItemsWithCategory_(sheet, items, category, importedIds, onImport, onSkip) {
  items.forEach(function(item) {
    const videoId = String(item.videoId || "").trim();
    const id = item.id || ("film_" + (videoId || Date.now() + "_" + Math.floor(Math.random() * 1000)));
    
    // Check duplicates by ID and Category
    const duplicate = findFilmByIdAndCategory_(sheet, id, category) || 
                      (videoId && findFilmByVideoIdAndCategory_(sheet, videoId, category));
    
    if (duplicate) {
      if (onSkip) onSkip();
      return;
    }

    let title = String(item.title || "").trim();
    let description = String(item.description || "").trim();
    let thumbnailUrl = String(item.thumbnailUrl || "").trim();
    let itemYear = String(item.year || "").trim();
    const videoUrl = item.url || (videoId ? "https://www.youtube.com/watch?v=" + videoId : "");

    if (videoId && (!title || !description || !thumbnailUrl || !itemYear)) {
      const meta = fetchYoutubeMetadata_(videoUrl);
      if (!title) title = String(meta.title || "").trim();
      if (!description) description = String(meta.description || "").trim();
      if (!thumbnailUrl) thumbnailUrl = String(meta.thumbnail_url || meta.thumbnailUrl || "").trim();
      if (!itemYear) itemYear = String(meta.year || "").trim();
    }

    const parsedYear = Number(itemYear);
    if (itemYear && !isNaN(parsedYear) && parsedYear < 2025) {
      if (onSkip) onSkip();
      return;
    }
    
    if (!title) {
      title = videoId ? "Video YouTube " + videoId : "Post YouTube";
    }
    if (!thumbnailUrl && videoId) {
      thumbnailUrl = buildYouTubeThumbnailUrl_(videoId);
    }

    sheet.appendRow([
      id,
      title,
      itemYear,
      description,
      category,
      videoUrl,
      videoId,
      thumbnailUrl,
      new Date(),
      "approved",
      0,
      0
    ]);
    if (onImport) onImport();
    if (videoId) importedIds.push(videoId);
  });
}

function normalizeYoutubeChannelUrl_(url) {
  const raw = String(url || "").trim();
  if (!raw) return "";
  if (raw.charAt(0) === "@") return "https://www.youtube.com/" + raw;
  if (/^https?:\/\//i.test(raw)) return raw;
  if (/^www\./i.test(raw)) return "https://" + raw;
  return "https://www.youtube.com/" + raw.replace(/^\/+/, "");
}

function resolveYoutubeChannelInfo_(channelUrl) {
  const normalizedUrl = normalizeYoutubeChannelUrl_(channelUrl);
  const directMatch = normalizedUrl.match(/youtube\.com\/channel\/(UC[A-Za-z0-9_-]{20,})/i);
  if (directMatch && directMatch[1]) {
    return { channelId: directMatch[1], title: "", url: "https://www.youtube.com/channel/" + directMatch[1] };
  }

  const html = fetchText_(normalizedUrl, "GET", null);
  const channelId = extractYoutubeChannelIdFromText_(html);
  const title = extractYoutubeChannelTitleFromHtml_(html);
  return {
    channelId: channelId,
    title: title,
    url: channelId ? "https://www.youtube.com/channel/" + channelId : normalizedUrl
  };
}

function extractYoutubeChannelIdFromText_(text) {
  const raw = String(text || "");
  const patterns = [
    /"channelId"\s*:\s*"(UC[A-Za-z0-9_-]{20,})"/,
    /"externalId"\s*:\s*"(UC[A-Za-z0-9_-]{20,})"/,
    /<meta[^>]+itemprop=["']channelId["'][^>]+content=["'](UC[A-Za-z0-9_-]{20,})["']/i,
    /\/channel\/(UC[A-Za-z0-9_-]{20,})/i
  ];
  for (let i = 0; i < patterns.length; i++) {
    const match = raw.match(patterns[i]);
    if (match && match[1]) return match[1];
  }
  return "";
}

function extractYoutubeChannelTitleFromHtml_(html) {
  const raw = String(html || "");
  const ogTitle = raw.match(/<meta[^>]+property=["']og:title["'][^>]+content=["']([^"']+)["']/i);
  if (ogTitle && ogTitle[1]) return decodeHtmlEntities_(ogTitle[1]);
  const title = raw.match(/<title>([^<]+)<\/title>/i);
  if (title && title[1]) return decodeHtmlEntities_(title[1]).replace(/\s*-\s*YouTube\s*$/i, "").trim();
  return "";
}

function fetchYoutubeChannelVideos_(channelId, maxItems) {
  const byId = {};
  const ordered = [];

  function addItem(item) {
    const videoId = String(item && item.videoId || "").trim();
    if (!videoId || byId[videoId]) return;
    byId[videoId] = {
      videoId: videoId,
      url: normalizeYoutubeUrl_(item.url || ("https://www.youtube.com/watch?v=" + videoId)),
      title: String(item.title || "").trim(),
      description: String(item.description || "").trim(),
      thumbnailUrl: String(item.thumbnailUrl || "").trim() || buildYouTubeThumbnailUrl_(videoId),
      year: String(item.year || "").trim()
    };
    ordered.push(byId[videoId]);
  }

  fetchYoutubeChannelRssVideos_(channelId).forEach(addItem);

  const html = fetchText_("https://www.youtube.com/channel/" + encodeURIComponent(channelId) + "/videos?view=0&sort=dd&flow=grid", "GET", null);
  parseYoutubeVideoRendererItems_(html).forEach(addItem);

  const apiKey = extractYoutubeInnertubeApiKey_(html);
  const clientVersion = extractYoutubeClientVersion_(html) || "2.20240601.00.00";
  let continuation = extractYoutubeContinuationToken_(html);
  let guard = 0;
  while (continuation && apiKey && ordered.length < maxItems && guard < 40) {
    guard++;
    const response = fetchYoutubeContinuation_(apiKey, clientVersion, continuation);
    parseYoutubeVideoRendererItems_(response).forEach(addItem);
    const next = extractYoutubeContinuationToken_(response);
    if (!next || next === continuation) break;
    continuation = next;
  }

  return ordered.slice(0, maxItems);
}

function fetchYoutubeChannelRssVideos_(channelId) {
  const items = [];
  try {
    const url = "https://www.youtube.com/feeds/videos.xml?channel_id=" + encodeURIComponent(channelId);
    const text = fetchText_(url, "GET", null);
    if (!text) return items;
    const document = XmlService.parse(text);
    const root = document.getRootElement();
    const atom = XmlService.getNamespace("http://www.w3.org/2005/Atom");
    const media = XmlService.getNamespace("media", "http://search.yahoo.com/mrss/");
    const yt = XmlService.getNamespace("yt", "http://www.youtube.com/xml/schemas/2015");
    const entries = root.getChildren("entry", atom);
    entries.forEach(function(entry) {
      const videoIdEl = entry.getChild("videoId", yt);
      const titleEl = entry.getChild("title", atom);
      const groupEl = entry.getChild("group", media);
      const descEl = groupEl ? groupEl.getChild("description", media) : null;
      const thumbEl = groupEl ? groupEl.getChild("thumbnail", media) : null;
      const publishedEl = entry.getChild("published", atom);
      const videoId = videoIdEl ? videoIdEl.getText() : "";
      const publishedText = publishedEl ? String(publishedEl.getText() || "") : "";
      const publishedYear = publishedText ? publishedText.substring(0, 4) : "";
      if (!videoId) return;
      items.push({
        videoId: videoId,
        url: "https://www.youtube.com/watch?v=" + videoId,
        title: titleEl ? titleEl.getText() : "",
        description: descEl ? descEl.getText() : "",
        thumbnailUrl: thumbEl ? String(thumbEl.getAttribute("url").getValue() || "") : "",
        year: publishedYear
      });
    });
  } catch (error) {
    Logger.log("YouTube RSS fetch failed: " + (error && error.message ? error.message : String(error)));
  }
  return items;
}

function parseYoutubeVideoRendererItems_(text) {
  const raw = unescapeYoutubeJsonText_(text);
  const items = [];
  const seen = {};
  const regex = /"videoId"\s*:\s*"([A-Za-z0-9_-]{11})"[\s\S]{0,2500}?"title"\s*:\s*\{[\s\S]{0,500}?"text"\s*:\s*"([^"]*)"/g;
  let match;
  while ((match = regex.exec(raw)) !== null) {
    const videoId = match[1];
    if (seen[videoId]) continue;
    seen[videoId] = true;
    items.push({
      videoId: videoId,
      url: "https://www.youtube.com/watch?v=" + videoId,
      title: decodeHtmlEntities_(unescapeYoutubeJsonText_(match[2] || "")),
      description: "",
      thumbnailUrl: buildYouTubeThumbnailUrl_(videoId),
      year: ""
    });
  }

  const simpleRegex = /"videoId"\s*:\s*"([A-Za-z0-9_-]{11})"/g;
  while ((match = simpleRegex.exec(raw)) !== null) {
    const videoId = match[1];
    if (seen[videoId]) continue;
    seen[videoId] = true;
    items.push({
      videoId: videoId,
      url: "https://www.youtube.com/watch?v=" + videoId,
      title: "",
      description: "",
      thumbnailUrl: buildYouTubeThumbnailUrl_(videoId),
      year: ""
    });
  }
  return items;
}

function parseYoutubePlaylists_(html) {
  const playlistIds = [];
  const raw = unescapeYoutubeJsonText_(html);
  const regex = /"playlistId"\s*:\s*"(PL[A-Za-z0-9_-]{18,})"/g;
  let match;
  const seen = {};
  while ((match = regex.exec(raw)) !== null) {
    const playlistId = match[1];
    if (seen[playlistId]) continue;
    seen[playlistId] = true;
    playlistIds.push(playlistId);
  }
  return playlistIds;
}

function parseYoutubeCommunityPosts_(html) {
  const posts = [];
  const raw = unescapeYoutubeJsonText_(html);
  
  const backstageRegex = /"backstagePostRenderer"\s*:\s*\{([\s\S]*?)\n\s*\}\s*,\s*"\w+"\s*:/g;
  let match;
  while ((match = backstageRegex.exec(raw)) !== null) {
    const postBody = match[1];
    
    const idMatch = postBody.match(/"postId"\s*:\s*"([^"]+)"/);
    const postId = idMatch ? idMatch[1] : "";
    if (!postId) continue;
    
    const textMatch = postBody.match(/"text"\s*:\s*"([^"]+)"/);
    const text = textMatch ? decodeHtmlEntities_(unescapeYoutubeJsonText_(textMatch[1])) : "";
    
    const dateMatch = postBody.match(/"publishedTimeText"\s*:\s*\{\s*"runs"\s*:\s*\[\s*\{\s*"text"\s*:\s*"([^"]+)"/);
    const dateText = dateMatch ? dateMatch[1] : "";
    
    posts.push({
      id: "post_" + postId,
      title: text.substring(0, 100) + (text.length > 100 ? "..." : ""),
      description: text,
      url: "https://www.youtube.com/post/" + postId,
      videoId: "",
      thumbnailUrl: "",
      year: dateText
    });
  }
  return posts;
}

function extractYoutubeInnertubeApiKey_(html) {
  const raw = String(html || "");
  const match = raw.match(/"INNERTUBE_API_KEY"\s*:\s*"([^"]+)"/);
  return match && match[1] ? match[1] : "";
}

function extractYoutubeClientVersion_(html) {
  const raw = String(html || "");
  const match = raw.match(/"clientVersion"\s*:\s*"([^"]+)"/);
  return match && match[1] ? match[1] : "";
}

function extractYoutubeContinuationToken_(text) {
  const raw = String(text || "");
  const regex = /"continuationCommand"\s*:\s*\{\s*"token"\s*:\s*"([^"]+)"/g;
  let match;
  let selected = "";
  while ((match = regex.exec(raw)) !== null) {
    const token = unescapeYoutubeJsonText_(match[1]);
    if (token && token !== selected) selected = token;
  }
  return selected;
}

function fetchYoutubeContinuation_(apiKey, clientVersion, continuation) {
  const body = JSON.stringify({
    context: {
      client: {
        clientName: "WEB",
        clientVersion: clientVersion
      }
    },
    continuation: continuation
  });
  return fetchText_("https://www.youtube.com/youtubei/v1/browse?key=" + encodeURIComponent(apiKey), "POST", body);
}

function fetchText_(url, method, body) {
  const options = {
    method: method || "GET",
    muteHttpExceptions: true,
    followRedirects: true,
    headers: {
      "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36",
      "Accept-Language": "id-ID,id;q=0.9,en-US;q=0.8,en;q=0.7"
    }
  };
  if (body) {
    options.contentType = "application/json";
    options.payload = body;
  }
  const response = UrlFetchApp.fetch(url, options);
  const code = response.getResponseCode();
  if (code < 200 || code >= 300) {
    Logger.log("Fetch failed " + code + " for " + url);
    return "";
  }
  return response.getContentText() || "";
}

function unescapeYoutubeJsonText_(text) {
  return String(text || "")
    .replace(/\\u0026/g, "&")
    .replace(/\\u003d/g, "=")
    .replace(/\\u002f/g, "/")
    .replace(/\\\"/g, "\"")
    .replace(/\\n/g, " ")
    .replace(/\\r/g, " ")
    .replace(/\\t/g, " ");
}

function handleLiveStreamUpdate_(payload) {
  validateActiveAdmin_(payload);

  const ss = SpreadsheetApp.openById(SPREADSHEET_ID);
  const sheet = ensureSheetWithHeaders_(ss, SHEET_FILMS, FILM_HEADERS);
  const uploadLink = String(
    payload.upload_link ||
    payload.link_upload ||
    payload.video_url ||
    payload.videoUrl ||
    payload.youtube_url ||
    payload.youtubeUrl ||
    payload.movie_url ||
    payload.live_url ||
    payload.liveUrl ||
    payload.stream_url ||
    payload.streamUrl ||
    ""
  ).trim();
  const normalizedUploadLink = normalizeUploadLink_(uploadLink);
  if (!normalizedUploadLink) return jsonOutput_({ success: false, message: "Link live wajib diisi." });

  const videoId = extractYouTubeVideoId_(normalizedUploadLink);
  const youtubeMeta = videoId ? fetchYoutubeMetadata_(normalizedUploadLink) : { title: "", description: "", thumbnailUrl: "", year: "" };
  const requestedTitle = String(payload.title || payload.judul || "").trim();
  const metaTitle = String(youtubeMeta.title || "").trim();
  const resolvedTitle = metaTitle || requestedTitle || inferLiveTitle_(normalizedUploadLink, "Live");
  const resolvedDeskripsi = String(youtubeMeta.description || payload.deskripsi || payload.synopsis || payload.description || "").trim();
  const resolvedYear = String(youtubeMeta.year || payload.year || payload.releaseYear || "").trim();
  let thumbnailUrl = String(youtubeMeta.thumbnailUrl || youtubeMeta.thumbnail_url || "").trim();
  if (!thumbnailUrl && videoId) thumbnailUrl = buildYouTubeThumbnailUrl_(videoId);

  const found = findFilmById_(sheet, ACTIVE_LIVE_STREAM_ID);
  const views = found && found.col["views"] !== undefined ? Number(found.row[found.col["views"]] || 0) : 0;
  const likes = found && found.col["likes"] !== undefined ? Number(found.row[found.col["likes"]] || 0) : 0;
  const row = [
    ACTIVE_LIVE_STREAM_ID,
    resolvedTitle,
    resolvedYear,
    resolvedDeskripsi,
    "Live",
    normalizedUploadLink,
    videoId,
    thumbnailUrl,
    new Date(),
    "approved",
    views,
    likes
  ];

  if (found) {
    sheet.getRange(found.rowNumber, 1, 1, FILM_HEADERS.length).setValues([row]);
  } else {
    sheet.appendRow(row);
  }

  return jsonOutput_({
    success: true,
    message: "Live streaming berhasil disimpan.",
    id: ACTIVE_LIVE_STREAM_ID,
    video_url: normalizedUploadLink,
    title: resolvedTitle
  });
}
function handleFilmRegistration_(payload) {
  const ss = SpreadsheetApp.openById(SPREADSHEET_ID);
  const sheet = ensureSheetWithHeaders_(ss, SHEET_FILMS, FILM_HEADERS);

  const title = String(payload.title || payload.judul || payload.judul_film || payload.songTitle || "").trim();
  const year = String(payload.year || payload.release || payload.releaseYear || payload.tahun_release || payload.tahun_pembuatan || "").trim();
  const deskripsi = String(payload.deskripsi || payload.synopsis || payload.description || payload.notes || "").trim();
  const uploadLink = String(
    payload.upload_link ||
    payload.link_upload ||
    payload.video_url ||
    payload.videoUrl ||
    payload.youtube_url ||
    payload.youtubeUrl ||
    payload.movie_url ||
    payload.live_url ||
    payload.liveUrl ||
    payload.stream_url ||
    payload.streamUrl ||
    ""
  ).trim();
  const normalizedUploadLink = normalizeUploadLink_(uploadLink);
  const videoId = extractYouTubeVideoId_(normalizedUploadLink);
  const youtubeMeta = videoId ? fetchYoutubeMetadata_(normalizedUploadLink) : { title: "", description: "", thumbnailUrl: "" };
  const kategori = String(payload.kategori || payload.category || payload.genre || "").trim() || inferLivePlatformLabel_(normalizedUploadLink);
  const resolvedTitle = title || String(youtubeMeta.title || "").trim() || inferLiveTitle_(normalizedUploadLink, kategori);
  const resolvedDeskripsi = deskripsi || String(youtubeMeta.description || "").trim();
  const resolvedYear = year || String(youtubeMeta.year || "").trim();
  const coverArtBase64 = String(payload.coverImageBase64 || payload.cover_art_base64 || payload.coverArtBase64 || "").trim();
  const coverArtFileName = String(payload.coverImageFileName || payload.cover_art_file_name || payload.coverArtFileName || "cover-art.jpg").trim() || "cover-art.jpg";
  const coverArtMimeType = String(payload.coverImageMimeType || payload.cover_art_mime_type || payload.coverArtMimeType || "image/jpeg").trim() || "image/jpeg";

  if (!normalizedUploadLink) return jsonOutput_({ success: false, message: "Link live wajib diisi." });

  const duplicate = videoId ? findFilmByVideoId_(sheet, videoId) : findFilmByUploadLink_(sheet, normalizedUploadLink);
  if (duplicate) {
    return jsonOutput_({ success: false, message: "Live ini sudah pernah dikirim." });
  }

  let thumbnailUrl = videoId ? String(youtubeMeta.thumbnailUrl || "").trim() : "";
  if (!thumbnailUrl && videoId) thumbnailUrl = buildYouTubeThumbnailUrl_(videoId);
  if (coverArtBase64) {
    thumbnailUrl = uploadBase64ImageToDrive_(
      coverArtBase64,
      coverArtFileName,
      coverArtMimeType,
      FILM_COVER_FOLDER_ID
    );
  }
  sheet.appendRow([
    "film_" + Date.now(),
    resolvedTitle,
    resolvedYear,
    resolvedDeskripsi,
    kategori,
    normalizedUploadLink,
    videoId,
    thumbnailUrl,
    new Date(),
    "approved",
    0,
    0
  ]);

  return jsonOutput_({ success: true, message: "Registrasi live berhasil disimpan." });
}

function handlePortalPost_(payload) {
  const ss = SpreadsheetApp.openById(SPREADSHEET_ID);
  const sheet = ensureSheetWithHeaders_(ss, SHEET_PORTAL, PORTAL_HEADERS);
  const user = validateActiveAdmin_(payload);
  const posterBase64 = String(
    payload.posterImageBase64 ||
    payload.imageBase64 ||
    payload.base64 ||
    payload.fileBase64 ||
    ""
  ).trim();
  if (!posterBase64) throw new Error("Base64 poster tidak ditemukan di payload.");

  const posterUrl = uploadBase64ImageToDrive_(
    posterBase64,
    payload.posterImageFileName || payload.fileName || "poster.jpg",
    payload.posterImageMimeType || payload.mimeType || "image/jpeg",
    PORTAL_POSTER_FOLDER_ID
  );
  const category = String(payload.category || "").trim();

  try {
    sheet.appendRow([
      "portal_" + Date.now(),
      user.username,
      category,
      posterUrl,
      new Date(),
      "ACTIVE"
    ]);
  } catch (error) {
    const detail = error && error.message ? error.message : String(error);
    throw new Error("Upload sukses, tapi gagal tulis ke sheet poster_posts: " + detail);
  }

  return jsonOutput_({ success: true, message: "Poster berhasil dipasang." });
}


function handlePortalDelete_(payload) {
  const postId = String(payload.postId || payload.post_id || payload.id || "").trim();
  if (!postId) return jsonOutput_({ success: false, message: "postId wajib diisi." });

  validateActiveAdmin_(payload);

  const ss = SpreadsheetApp.openById(SPREADSHEET_ID);
  const sheet = ensureSheetWithHeaders_(ss, SHEET_PORTAL, PORTAL_HEADERS);
  const values = sheet.getDataRange().getValues();
  if (values.length <= 1) {
    return jsonOutput_({ success: false, message: "Data informasi kosong." });
  }

  const col = getSheetHeaderMap_(values[0]);
  const idIdx = col["id"];
  const posterIdx = col["poster_url"];
  if (idIdx === undefined) {
    return jsonOutput_({ success: false, message: "Kolom id tidak ditemukan." });
  }

  for (let r = 1; r < values.length; r++) {
    const rowId = String(values[r][idIdx] || "").trim();
    if (rowId !== postId) continue;

    if (posterIdx !== undefined) {
      const posterUrl = String(values[r][posterIdx] || "").trim();
      if (posterUrl) deletePosterFileByUrl_(posterUrl);
    }

    sheet.deleteRow(r + 1);
    return jsonOutput_({ success: true, message: "Informasi berhasil dihapus.", deleted_id: postId });
  }

  return jsonOutput_({ success: false, message: "Informasi tidak ditemukan." });
}
function handlePortalReport_(payload) {
  const ss = SpreadsheetApp.openById(SPREADSHEET_ID);
  const sheet = ensureSheetWithHeaders_(ss, SHEET_PORTAL_REPORTS, PORTAL_REPORT_HEADERS);
  const reason = String(payload.reason || "").trim();
  const postId = String(payload.postId || "").trim();
  const postTitle = String(payload.postTitle || payload.title || "").trim();
  const category = String(payload.category || "").trim();
  const reportedLink = String(payload.reportedLink || payload.reportedDriveLink || "").trim();
  const posterUrl = String(payload.posterUrl || payload.poster_url || getPortalPosterUrlById_(postId) || "").trim();

  if (!postId) return jsonOutput_({ success: false, message: "postId wajib diisi." });
  if (!reason) return jsonOutput_({ success: false, message: "reason wajib diisi." });

  sheet.appendRow([
    "report_" + Date.now(),
    postId,
    postTitle,
    category,
    reason,
    reportedLink,
    posterUrl,
    "NEW"
  ]);

  return jsonOutput_({ success: true, message: "Laporan diterima. Terima kasih." });
}

function handleContentReport_(payload) {
  const targetType = String(payload.targetType || "").trim().toLowerCase();
  const targetId = String(payload.targetId || "").trim();
  const reason = String(payload.reason || "").trim();
  const reportedLink = String(payload.reportedLink || payload.reportedDriveLink || "").trim();
  const posterUrl = String(payload.posterUrl || payload.poster_url || (targetType === "poster" ? getPortalPosterUrlById_(targetId) : "") || "").trim();

  if (!targetId) return jsonOutput_({ success: false, message: "targetId wajib diisi." });
  if (!reason) return jsonOutput_({ success: false, message: "reason wajib diisi." });
  if (targetType !== "film" && targetType !== "poster") {
    return jsonOutput_({ success: false, message: "targetType harus film atau poster." });
  }

  const ss = SpreadsheetApp.openById(SPREADSHEET_ID);
  const reportsSheet = ensureSheetWithHeaders_(ss, SHEET_PORTAL_REPORTS, PORTAL_REPORT_HEADERS);
  reportsSheet.appendRow([
    "report_" + Date.now(),
    targetId,
    String(payload.postTitle || payload.title || "").trim(),
    String(payload.category || targetType || "").trim(),
    "[" + targetType + "] " + reason,
    reportedLink,
    posterUrl,
    "NEW"
  ]);

  return jsonOutput_({ success: true, message: "Laporan diterima." });
}

function handleFilmLike_(payload) {
  const filmId = String(payload.film_id || payload.movie_id || payload.id || "").trim();
  if (!filmId) return jsonOutput_({ success: false, message: "film_id wajib diisi." });
  validateActiveAdmin_(payload);

  const ss = SpreadsheetApp.openById(SPREADSHEET_ID);
  const filmsSheet = ensureSheetWithHeaders_(ss, SHEET_FILMS, FILM_HEADERS);
  if (!findFilmById_(filmsSheet, filmId)) {
    return jsonOutput_({ success: false, message: "Film tidak ditemukan." });
  }

  const totalLikes = incrementFilmLikesColumn_(filmsSheet, filmId);
  return jsonOutput_({
    success: true,
    already_liked: false,
    total_likes: totalLikes,
    likes: totalLikes
  });
}

function handleFilmView_(payload) {
  const filmId = String(payload.film_id || payload.movie_id || payload.id || "").trim();
  const videoUrl = String(payload.video_url || payload.videoUrl || payload.youtube_url || payload.youtubeUrl || payload.movie_url || "").trim();
  const videoId = String(payload.video_id || payload.videoId || extractYouTubeVideoId_(videoUrl) || "").trim();
  if (!filmId && !videoId && !videoUrl) {
    return jsonOutput_({ success: false, message: "film_id, video_id, atau video_url wajib diisi." });
  }

  const ss = SpreadsheetApp.openById(SPREADSHEET_ID);
  const sheet = ensureSheetWithHeaders_(ss, SHEET_FILMS, FILM_HEADERS);
  const values = sheet.getDataRange().getValues();
  if (values.length <= 1) return jsonOutput_({ success: false, message: "Data film kosong." });

  const col = getSheetHeaderMap_(values[0]);
  const idIdx = col["id"];
  const videoIdIdx = col["video_id"];
  const videoUrlIdx = col["video_url"] !== undefined ? col["video_url"] : col["youtube_url"];
  const viewsIdx = col["views"];
  if (viewsIdx === undefined) {
    return jsonOutput_({ success: false, message: "Kolom views tidak ditemukan." });
  }

  for (let r = 1; r < values.length; r++) {
    const rowId = idIdx !== undefined ? String(values[r][idIdx] || "").trim() : "";
    const rowVideoId = videoIdIdx !== undefined ? String(values[r][videoIdIdx] || "").trim() : "";
    const rowVideoUrl = videoUrlIdx !== undefined ? String(values[r][videoUrlIdx] || "").trim() : "";
    const rowExtractedVideoId = extractYouTubeVideoId_(rowVideoUrl);
    const matchedById = filmId && rowId === filmId;
    const matchedByVideoId = videoId && (rowVideoId === videoId || rowExtractedVideoId === videoId);
    const matchedByVideoUrl = videoUrl && rowVideoUrl === videoUrl;
    if (!matchedById && !matchedByVideoId && !matchedByVideoUrl) continue;

    const current = Number(values[r][viewsIdx] || 0);
    const next = Math.max(0, current + 1);
    sheet.getRange(r + 1, viewsIdx + 1).setValue(next);
    return jsonOutput_({ success: true, views: next });
  }

  return jsonOutput_({ success: false, message: "Film tidak ditemukan." });
}

function handleFilmUpdate_(payload) {
  const filmId = String(payload.film_id || payload.movie_id || payload.id || "").trim();
  const title = String(payload.title || payload.judul || payload.judul_film || "").trim();
  const year = String(payload.year || payload.release || payload.releaseYear || payload.tahun_release || payload.tahun_pembuatan || "").trim();
  const deskripsi = String(payload.deskripsi || payload.synopsis || payload.description || payload.notes || "").trim();
  if (!filmId) return jsonOutput_({ success: false, message: "film_id wajib diisi." });
  if (!title) return jsonOutput_({ success: false, message: "Judul wajib diisi." });

  validateActiveAdmin_(payload);

  const ss = SpreadsheetApp.openById(SPREADSHEET_ID);
  const sheet = ensureSheetWithHeaders_(ss, SHEET_FILMS, FILM_HEADERS);
  const found = findFilmById_(sheet, filmId);
  if (!found) {
    return jsonOutput_({ success: false, message: "Film tidak ditemukan." });
  }

  const titleIdx = found.col["title"];
  const yearIdx = found.col["year"];
  const descIdx = found.col["deskripsi"];
  if (titleIdx !== undefined) sheet.getRange(found.rowNumber, titleIdx + 1).setValue(title);
  if (yearIdx !== undefined) sheet.getRange(found.rowNumber, yearIdx + 1).setValue(year);
  if (descIdx !== undefined) sheet.getRange(found.rowNumber, descIdx + 1).setValue(deskripsi);

  return jsonOutput_({
    success: true,
    message: "Metadata film berhasil diperbarui.",
    film: {
      id: filmId,
      title: title,
      year: year,
      deskripsi: deskripsi,
      synopsis: deskripsi,
      description: deskripsi
    }
  });
}


function handleFilmDelete_(payload) {
  const filmId = String(payload.film_id || payload.movie_id || payload.id || "").trim();
  const videoUrl = String(payload.video_url || payload.videoUrl || payload.youtube_url || payload.youtubeUrl || payload.movie_url || "").trim();
  const normalizedVideoUrl = normalizeUploadLink_(videoUrl);
  const videoId = String(payload.video_id || payload.videoId || extractYouTubeVideoId_(normalizedVideoUrl) || "").trim();
  if (!filmId && !videoId && !normalizedVideoUrl) {
    return jsonOutput_({ success: false, message: "film_id, video_id, atau video_url wajib diisi." });
  }

  validateActiveAdmin_(payload);

  const ss = SpreadsheetApp.openById(SPREADSHEET_ID);
  const sheet = ensureSheetWithHeaders_(ss, SHEET_FILMS, FILM_HEADERS);
  let found = filmId ? findFilmById_(sheet, filmId) : null;
  if (!found && videoId) found = findFilmByVideoId_(sheet, videoId);
  if (!found && normalizedVideoUrl) found = findFilmByUploadLink_(sheet, normalizedVideoUrl);
  if (!found) {
    return jsonOutput_({ success: false, message: "Film tidak ditemukan." });
  }

  sheet.deleteRow(found.rowNumber);

  return jsonOutput_({
    success: true,
    message: "Film berhasil dihapus.",
    deleted_id: filmId || videoId || normalizedVideoUrl
  });
}
function normalizeYoutubeUrl_(url) {
  const raw = String(url || "").trim();
  if (!raw) return "";
  const videoId = extractYouTubeVideoId_(raw);
  if (!videoId) return raw;
  return "https://www.youtube.com/watch?v=" + videoId;
}

function normalizeUploadLink_(url) {
  const raw = String(url || "").trim();
  if (!raw) return "";
  const videoId = extractYouTubeVideoId_(raw);
  if (videoId) return "https://www.youtube.com/watch?v=" + videoId;
  return raw;
}

function inferLivePlatformLabel_(url) {
  const raw = String(url || "").trim();
  if (!raw) return "Live";
  try {
    const parsed = new URL(raw);
    const host = String(parsed.hostname || "").toLowerCase();
    if (host.indexOf("youtube.com") >= 0 || host.indexOf("youtu.be") >= 0) return "YouTube";
    if (host.indexOf("instagram.com") >= 0) return "Instagram";
    if (host.indexOf("facebook.com") >= 0 || host.indexOf("fb.watch") >= 0) return "Facebook";
    return host.replace(/^www\./, "") || "Live";
  } catch (error) {
    return "Live";
  }
}

function inferLiveTitle_(url, platformLabel) {
  const platform = String(platformLabel || "").trim() || inferLivePlatformLabel_(url);
  if (!platform || platform.toLowerCase() === "live") return "Live Streaming";
  return platform + " Live";
}

function extractYouTubeVideoId_(url) {
  const raw = String(url || "").trim();
  if (!raw) return "";

  const watchMatch = raw.match(/[?&]v=([A-Za-z0-9_-]{6,})/i);
  if (watchMatch && watchMatch[1]) return watchMatch[1];

  const shortsMatch = raw.match(/youtube\.com\/shorts\/([A-Za-z0-9_-]{6,})/i);
  if (shortsMatch && shortsMatch[1]) return shortsMatch[1];

  const youtuBeMatch = raw.match(/youtu\.be\/([A-Za-z0-9_-]{6,})/i);
  if (youtuBeMatch && youtuBeMatch[1]) return youtuBeMatch[1];

  try {
    const parsed = new URL(raw);
    const host = String(parsed.hostname || "").toLowerCase();
    if (host.indexOf("youtube.com") >= 0) {
      const v = parsed.searchParams.get("v");
      if (v) return v.trim();
      const pathParts = parsed.pathname.split("/").filter(Boolean);
      const shortsIndex = pathParts.indexOf("shorts");
      if (shortsIndex >= 0 && shortsIndex + 1 < pathParts.length) {
        return pathParts[shortsIndex + 1].trim();
      }
    }
    if (host.indexOf("youtu.be") >= 0) {
      const pathParts = parsed.pathname.split("/").filter(Boolean);
      if (pathParts.length) return pathParts[0].trim();
    }
  } catch (error) {}

  return "";
}

function buildYouTubeThumbnailUrl_(videoId) {
  const id = String(videoId || "").trim();
  if (!id) return "";
  const maxResUrl = "https://img.youtube.com/vi/" + id + "/maxresdefault.jpg";
  const fallbackUrl = "https://img.youtube.com/vi/" + id + "/hqdefault.jpg";
  try {
    const response = UrlFetchApp.fetch(maxResUrl, { muteHttpExceptions: true, followRedirects: true });
    if (response.getResponseCode() >= 200 && response.getResponseCode() < 300) {
      return maxResUrl;
    }
  } catch (error) {
    Logger.log("Thumbnail fetch failed for videoId=" + id + ": " + (error && error.message ? error.message : String(error)));
  }
  return fallbackUrl;
}

function fetchYoutubeMetadata_(youtubeUrl) {
  const normalizedUrl = normalizeYoutubeUrl_(youtubeUrl);
  const videoId = extractYouTubeVideoId_(normalizedUrl);
  if (!videoId) {
    return {
      success: false,
      message: "Link YouTube tidak valid.",
      title: "",
      description: "",
      year: "",
      thumbnail_url: "",
      video_id: ""
    };
  }

  const oembedUrl = "https://www.youtube.com/oembed?url=" + encodeURIComponent(normalizedUrl) + "&format=json";
  let title = "";
  let thumbnailUrl = "";
  try {
    const response = UrlFetchApp.fetch(oembedUrl, { muteHttpExceptions: true, followRedirects: true });
    if (response.getResponseCode() >= 200 && response.getResponseCode() < 300) {
      const data = JSON.parse(response.getContentText() || "{}");
      title = String(data.title || "").trim();
      thumbnailUrl = String(data.thumbnail_url || "").trim();
    }
  } catch (error) {
    Logger.log("YouTube oEmbed failed for " + videoId + ": " + (error && error.message ? error.message : String(error)));
  }

  let description = "";
  let year = "";
  try {
    const pageUrl = normalizedUrl.indexOf("?") >= 0 ? normalizedUrl + "&hl=id" : normalizedUrl + "?hl=id";
    const response = UrlFetchApp.fetch(pageUrl, {
      muteHttpExceptions: true,
      followRedirects: true,
      headers: {
        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
      }
    });
    if (response.getResponseCode() >= 200 && response.getResponseCode() < 300) {
      const html = response.getContentText() || "";
      const metaDescription = html.match(/<meta[^>]+property=["']og:description["'][^>]+content=["']([^"']+)["']/i);
      if (metaDescription && metaDescription[1]) {
        description = decodeHtmlEntities_(metaDescription[1].trim());
      }
      const published = html.match(/<meta[^>]+itemprop=["']datePublished["'][^>]+content=["']([^"']+)["']/i) || html.match(/"publishDate"\s*:\s*"(\d{4}-\d{2}-\d{2})"/i) || html.match(/"datePublished"\s*:\s*"(\d{4}-\d{2}-\d{2})"/i);
      if (published && published[1]) year = String(published[1]).substring(0, 4);
      if (!title) {
        const metaTitle = html.match(/<meta[^>]+property=["']og:title["'][^>]+content=["']([^"']+)["']/i);
        if (metaTitle && metaTitle[1]) {
          title = decodeHtmlEntities_(metaTitle[1].trim());
        }
      }
    }
  } catch (error) {
    Logger.log("YouTube page fetch failed for " + videoId + ": " + (error && error.message ? error.message : String(error)));
  }

  return {
    success: true,
    title: title,
    description: description,
    year: year,
    thumbnail_url: thumbnailUrl || buildYouTubeThumbnailUrl_(videoId),
    video_id: videoId,
    video_url: normalizedUrl,
    youtube_url: normalizedUrl
  };
}

function findFilmByVideoId_(sheet, videoId) {
  const trimmedVideoId = String(videoId || "").trim();
  if (!trimmedVideoId) return null;
  const values = sheet.getDataRange().getValues();
  if (values.length <= 1) return null;
  const col = getSheetHeaderMap_(values[0]);
  const videoIdIdx = col["video_id"];
  const videoUrlIdx = col["video_url"] !== undefined ? col["video_url"] : col["youtube_url"];
  if (videoIdIdx === undefined && videoUrlIdx === undefined) return null;

  for (let r = 1; r < values.length; r++) {
    const rowVideoId = videoIdIdx !== undefined ? String(values[r][videoIdIdx] || "").trim() : "";
    const rowVideoUrl = videoUrlIdx !== undefined ? String(values[r][videoUrlIdx] || "").trim() : "";
    const rowExtractedVideoId = extractYouTubeVideoId_(rowVideoUrl);
    if (rowVideoId === trimmedVideoId || rowExtractedVideoId === trimmedVideoId) {
      return { rowNumber: r + 1, row: values[r], col: col };
    }
  }
  return null;
}

function findFilmByUploadLink_(sheet, uploadLink) {
  const trimmedUploadLink = String(uploadLink || "").trim();
  if (!trimmedUploadLink) return null;
  const values = sheet.getDataRange().getValues();
  if (values.length <= 1) return null;
  const col = getSheetHeaderMap_(values[0]);
  const videoUrlIdx = col["video_url"] !== undefined ? col["video_url"] : col["youtube_url"];
  if (videoUrlIdx === undefined) return null;

  for (let r = 1; r < values.length; r++) {
    const rowVideoUrl = String(values[r][videoUrlIdx] || "").trim();
    if (rowVideoUrl === trimmedUploadLink) {
      return { rowNumber: r + 1, row: values[r], col: col };
    }
  }
  return null;
}

function findFilmByIdAndCategory_(sheet, id, category) {
  const trimmedId = String(id || "").trim();
  if (!trimmedId) return null;
  const values = sheet.getDataRange().getValues();
  if (values.length <= 1) return null;
  const col = getSheetHeaderMap_(values[0]);
  const idIdx = col["id"];
  const kategoriIdx = col["kategori"] !== undefined ? col["kategori"] : col["category"];
  if (idIdx === undefined) return null;

  for (let r = 1; r < values.length; r++) {
    const rowId = String(values[r][idIdx] || "").trim();
    const rowKategori = kategoriIdx !== undefined ? String(values[r][kategoriIdx] || "").trim() : "";
    if (rowId === trimmedId && rowKategori.toLowerCase() === category.toLowerCase()) {
      return { rowNumber: r + 1, row: values[r], col: col };
    }
  }
  return null;
}

function findFilmByVideoIdAndCategory_(sheet, videoId, category) {
  const trimmedVideoId = String(videoId || "").trim();
  if (!trimmedVideoId) return null;
  const values = sheet.getDataRange().getValues();
  if (values.length <= 1) return null;
  const col = getSheetHeaderMap_(values[0]);
  const videoIdIdx = col["video_id"];
  const videoUrlIdx = col["video_url"] !== undefined ? col["video_url"] : col["youtube_url"];
  const kategoriIdx = col["kategori"] !== undefined ? col["kategori"] : col["category"];
  if (videoIdIdx === undefined && videoUrlIdx === undefined) return null;

  for (let r = 1; r < values.length; r++) {
    const rowVideoId = videoIdIdx !== undefined ? String(values[r][videoIdIdx] || "").trim() : "";
    const rowVideoUrl = videoUrlIdx !== undefined ? String(values[r][videoUrlIdx] || "").trim() : "";
    const rowExtractedVideoId = extractYouTubeVideoId_(rowVideoUrl);
    const rowKategori = kategoriIdx !== undefined ? String(values[r][kategoriIdx] || "").trim() : "";
    if ((rowVideoId === trimmedVideoId || rowExtractedVideoId === trimmedVideoId) && rowKategori.toLowerCase() === category.toLowerCase()) {
      return { rowNumber: r + 1, row: values[r], col: col };
    }
  }
  return null;
}

function getPortalPosterUrlById_(postId) {
  const id = String(postId || "").trim();
  if (!id) return "";
  const ss = SpreadsheetApp.openById(SPREADSHEET_ID);
  const sheet = ensureSheetWithHeaders_(ss, SHEET_PORTAL, PORTAL_HEADERS);
  const rows = readSheetObjects_(sheet);
  for (let i = 0; i < rows.length; i++) {
    const rowId = String(rows[i].id || "").trim();
    if (rowId === id) return String(rows[i].poster_url || "").trim();
  }
  return "";
}

function diagnoseDriveAccess() {
  try {
    const folder = DriveApp.getFolderById(PORTAL_POSTER_FOLDER_ID);
    return {
      success: true,
      message: "DriveApp OK",
      folderName: folder.getName(),
      folderId: PORTAL_POSTER_FOLDER_ID
    };
  } catch (error) {
    return {
      success: false,
      message: "DriveApp belum punya izin. Jalankan manual function diagnoseDriveAccess() lalu Authorize, kemudian deploy ulang Web App as Execute as: Me.",
      detail: error && error.message ? error.message : String(error)
    };
  }
}

function getApprovedMovies_(likeUserId) {
  const ss = SpreadsheetApp.openById(SPREADSHEET_ID);
  const sheet = ensureSheetWithHeaders_(ss, SHEET_FILMS, FILM_HEADERS);
  return readSheetObjects_(sheet)
    .filter(function(movie) {
      return String(movie.status || "").trim().toLowerCase() === "approved";
    })
    .sort(function(a, b) {
      return parseDateValue_(b.created_at) - parseDateValue_(a.created_at);
    })
    .map(function(movie) {
      const movieId = String(movie.id || "").trim();
      const totalLikes = Number(movie.likes || 0);
      const title = String(movie.title || movie.judul || movie.judul_film || "").trim();
      const year = String(movie.year || movie.release || movie.releaseYear || movie.tahun_release || movie.tahun_pembuatan || "").trim();
      const deskripsi = String(movie.deskripsi || movie.synopsis || movie.description || "").trim();
      const kategori = String(movie.kategori || movie.category || movie.genre || "").trim() || "Live";
      const videoUrl = String(movie.video_url || movie.videoUrl || movie.youtube_url || movie.youtubeUrl || movie.movie_url || "").trim();
      const videoId = String(movie.video_id || movie.videoId || "").trim();
      const thumbnailUrl = String(movie.thumbnail_url || movie.thumbnailUrl || movie.cover_url || movie.poster_url || "").trim();
      return {
        id: movieId,
        film_id: movieId,
        title: title,
        judul: title,
        judul_film: title,
        year: year,
        release: year,
        releaseYear: year,
        deskripsi: deskripsi,
        synopsis: deskripsi,
        description: deskripsi,
        kategori: kategori,
        category: kategori,
        video_url: videoUrl,
        videoUrl: videoUrl,
        youtube_url: videoUrl,
        youtubeUrl: videoUrl,
        video_id: videoId,
        videoId: videoId,
        thumbnail_url: thumbnailUrl,
        thumbnailUrl: thumbnailUrl,
        cover_url: thumbnailUrl,
        poster_url: thumbnailUrl,
        movie_url: videoUrl,
        status: String(movie.status || "").trim(),
        views: Number(movie.views || 0),
        likes: totalLikes,
        total_likes: totalLikes,
        already_liked: false
      };
    });
}

function findFilmById_(filmsSheet, filmId) {
  const values = filmsSheet.getDataRange().getValues();
  if (values.length <= 1) return null;
  const col = getSheetHeaderMap_(values[0]);
  const idIdx = col["id"];
  if (idIdx === undefined) return null;
  for (let r = 1; r < values.length; r++) {
    if (String(values[r][idIdx] || "").trim() === filmId) {
      return { rowNumber: r + 1, row: values[r], col: col };
    }
  }
  return null;
}

function incrementFilmLikesColumn_(filmsSheet, filmId) {
  const found = findFilmById_(filmsSheet, filmId);
  if (!found) return 0;
  const likesIdx = found.col["likes"];
  if (likesIdx === undefined) return 0;
  const nextLikes = Math.max(0, Number(found.row[likesIdx] || 0) + 1);
  filmsSheet.getRange(found.rowNumber, likesIdx + 1).setValue(nextLikes);
  return nextLikes;
}

function getApprovedCategories_(movies) {
  const set = {};
  movies.forEach(function(movie) {
    const kategori = String(movie.kategori || movie.category || movie.genre || "").trim();
    if (!kategori) return;
    set[kategori] = true;
  });
  return ["Semua"].concat(Object.keys(set).sort());
}

function getActivePortalPosts_() {
  const ss = SpreadsheetApp.openById(SPREADSHEET_ID);
  const sheet = ensureSheetWithHeaders_(ss, SHEET_PORTAL, PORTAL_HEADERS);
  expirePortalPostsIfNeeded_(sheet);
  return readSheetObjects_(sheet)
    .filter(function(post) {
      return String(post.status || "").trim().toUpperCase() === "ACTIVE";
    })
    .map(function(post) {
      const creatorName = String(post.user_id || "").trim();
      return {
        id: post.id,
        title: post.category || creatorName || "",
        band_name: creatorName,
        creator_name: creatorName,
        submitter_name: creatorName,
        category: post.category || "",
        poster_url: post.poster_url || "",
        short_caption: "",
        caption: "",
        event_date: "",
        posting_at: formatDateValue_(post.posting_at),
        status: post.status || "",
        created_at: formatDateValue_(post.posting_at)
      };
    });
}

function expirePortalPostsIfNeeded_(sheet) {
  const values = sheet.getDataRange().getValues();
  if (values.length <= 1) return;
  const col = getSheetHeaderMap_(values[0]);
  const statusIdx = col["status"];
  const postingAtIdx = col["posting_at"];
  const posterUrlIdx = col["poster_url"];
  if (statusIdx === undefined || postingAtIdx === undefined) return;

  const nowMs = Date.now();
  const expireMs = PORTAL_POST_EXPIRE_HOURS * 60 * 60 * 1000;
  for (let r = 1; r < values.length; r++) {
    const status = String(values[r][statusIdx] || "").trim().toUpperCase();
    if (status !== "ACTIVE") continue;
    const postingMs = parseDateValue_(values[r][postingAtIdx]);
    if (!postingMs) continue;
    if (nowMs - postingMs >= expireMs) {
      if (posterUrlIdx !== undefined) {
        const posterUrl = String(values[r][posterUrlIdx] || "").trim();
        deletePosterFileByUrl_(posterUrl);
      }
      sheet.getRange(r + 1, statusIdx + 1).setValue("EXPIRED");
    }
  }
}

function deletePosterFileByUrl_(url) {
  const fileId = extractDriveFileIdFromUrl_(url);
  if (!fileId) return;
  try {
    const file = DriveApp.getFileById(fileId);
    file.setTrashed(true);
  } catch (error) {
    Logger.log("Gagal hapus file expired: " + (error && error.message ? error.message : String(error)));
  }
}

function extractDriveFileIdFromUrl_(url) {
  const raw = String(url || "").trim();
  if (!raw) return "";

  const byIdParam = raw.match(/[?&]id=([a-zA-Z0-9_-]{10,})/);
  if (byIdParam && byIdParam[1]) return byIdParam[1];

  const byFilePath = raw.match(/\/file\/d\/([a-zA-Z0-9_-]{10,})/);
  if (byFilePath && byFilePath[1]) return byFilePath[1];

  const byUcPath = raw.match(/\/uc\?[^#]*id=([a-zA-Z0-9_-]{10,})/);
  if (byUcPath && byUcPath[1]) return byUcPath[1];

  return "";
}

function decodeHtmlEntities_(text) {
  return String(text || "")
    .replace(/&amp;/g, "&")
    .replace(/&quot;/g, "\"")
    .replace(/&#39;/g, "'")
    .replace(/&lt;/g, "<")
    .replace(/&gt;/g, ">");
}

function getPortalCreatorName_(post) {
  return String(post.user_id || post.creator_name || post.band_name || "").trim();
}

function getPortalCaption_(post) {
  return String(post.caption || post.short_caption || "").trim();
}

function handleAdminLogin_(payload) {
  const username = normalizeAdminUsername_(payload.username || payload.userId || payload.bandName || "");
  const password = String(payload.password || "").trim();
  const admin = findAdminByUsernameForLogin_(username, password);
  if (!admin) return jsonOutput_({ success: false, message: "Admin tidak ditemukan." });
  if (normalizeAdminPassword_(admin.password) !== normalizeAdminPassword_(password)) {
    return jsonOutput_({ success: false, message: "Password salah." });
  }

  return jsonOutput_({
    success: true,
    message: "Login berhasil.",
    user: {
      id: admin.username,
      username: admin.username,
      bandName: admin.username,
      nama_anggota: admin.username,
      whatsapp: admin.username,
      createdAt: formatDateValue_(new Date()),
      status: "ACTIVE"
    }
  });
}


function handleAdminUpdateCredentials_(payload) {
  const currentUsername = normalizeAdminUsername_(payload.username || payload.userId || payload.bandName || "");
  const newUsername = normalizeAdminUsername_(payload.new_username || payload.newUsername || "");
  const newPassword = normalizeAdminPassword_(payload.new_password || payload.newPassword || "");

  if (!currentUsername) return jsonOutput_({ success: false, message: "Username admin aktif tidak ditemukan." });
  

  const ss = SpreadsheetApp.openById(SPREADSHEET_ID);
  const sheet = ensureAdminSheetWithDefaults_(ss);
  const values = sheet.getDataRange().getValues();
  const col = getSheetHeaderMap_(values[0]);
  const usernameIdx = col["username"];
  const passwordIdx = col["password"];

  if (usernameIdx === undefined || passwordIdx === undefined) {
    return jsonOutput_({ success: false, message: "Kolom username/password tidak ditemukan di sheet admin." });
  }

  let targetRow = -1;
  let duplicateRow = -1;

  for (let i = 1; i < values.length; i++) {
    const rowUsername = normalizeAdminUsername_(String(values[i][usernameIdx] || ""));
    if (rowUsername === currentUsername) targetRow = i + 1;
    if (rowUsername === newUsername) duplicateRow = i + 1;
  }

  if (targetRow < 2) {
    return jsonOutput_({ success: false, message: "Admin tidak ditemukan." });
  }

  if (duplicateRow > 0 && duplicateRow !== targetRow) {
    return jsonOutput_({ success: false, message: "Username baru sudah dipakai admin lain." });
  }

  sheet.getRange(targetRow, usernameIdx + 1).setValue(newUsername);
  sheet.getRange(targetRow, passwordIdx + 1).setValue(newPassword);

  return jsonOutput_({
    success: true,
    message: "Username dan password admin berhasil diperbarui.",
    user: {
      id: newUsername,
      username: newUsername,
      bandName: newUsername,
      nama_anggota: newUsername,
      whatsapp: newUsername,
      createdAt: formatDateValue_(new Date()),
      status: "ACTIVE"
    }
  });
}
function handleAdminCheckStatus_(payload) {
  const username = normalizeAdminUsername_(payload.username || payload.userId || payload.bandName || "");
  const admin = findAdminByUsername_(username);
  if (!admin) return jsonOutput_({ success: false, message: "Info admin tidak ditemukan." });
  return jsonOutput_({
    success: true,
    message: "Status ditemukan.",
    user: {
      id: admin.username,
      username: admin.username,
      bandName: admin.username,
      nama_anggota: admin.username,
      whatsapp: admin.username,
      createdAt: "",
      status: "ACTIVE"
    }
  });
}

function validateActiveAdmin_(payload) {
  const username = normalizeAdminUsername_(payload.username || payload.userId || payload.bandName || "");
  const password = String(payload.password || "").trim();
  if (!username) throw new Error("Login admin diperlukan.");

  const admin = password ? findAdminByUsernameForLogin_(username, password) : findAdminByUsername_(username);
  if (!admin) throw new Error("Admin tidak ditemukan.");
  if (normalizeAdminUsername_(admin.username) !== username) throw new Error("Data admin tidak cocok.");
  if (password && normalizeAdminPassword_(admin.password) !== normalizeAdminPassword_(password)) {
    throw new Error("Password admin tidak cocok.");
  }
  return {
    username: admin.username,
    password: admin.password,
    status: "ACTIVE"
  };
}


function findAdminByUsernameForLogin_(username, password) {
  const normalized = normalizeAdminUsername_(username);
  const normalizedPassword = normalizeAdminPassword_(password);
  const ss = SpreadsheetApp.openById(SPREADSHEET_ID);
  const sheet = ensureAdminSheetWithDefaults_(ss);
  const values = sheet.getDataRange().getValues();
  if (values.length <= 1) {
    if (normalized === "admin") return { username: "admin", password: "admin" };
    if (normalized === "" && normalizedPassword === "") return { username: "", password: "" };
    return null;
  }

  const col = getSheetHeaderMap_(values[0]);
  const usernameIdx = col["username"];
  const passwordIdx = col["password"];
  if (usernameIdx === undefined || passwordIdx === undefined) return null;

  for (let i = 1; i < values.length; i++) {
    const rowUsername = normalizeAdminUsername_(String(values[i][usernameIdx] || ""));
    const rowPassword = normalizeAdminPassword_(String(values[i][passwordIdx] || ""));
    if (rowUsername === normalized && rowPassword === normalizedPassword) {
      return {
        username: String(values[i][usernameIdx] || "").trim(),
        password: String(values[i][passwordIdx] || "").trim()
      };
    }
  }
  return null;
}
function findAdminByUsername_(username) {
  const normalized = normalizeAdminUsername_(username);
  const ss = SpreadsheetApp.openById(SPREADSHEET_ID);
  const sheet = ensureAdminSheetWithDefaults_(ss);
  const values = sheet.getDataRange().getValues();
  if (values.length <= 1) {
    return normalized === "admin" ? { username: "admin", password: "admin" } : null;
  }
  const col = getSheetHeaderMap_(values[0]);
  const usernameIdx = col["username"];
  const passwordIdx = col["password"];
  if (usernameIdx === undefined || passwordIdx === undefined) return null;

  for (let rowIndex = 1; rowIndex < values.length; rowIndex++) {
    const rowUsername = normalizeAdminUsername_(String(values[rowIndex][usernameIdx] || ""));
    if (rowUsername === normalized) {
      return {
        username: String(values[rowIndex][usernameIdx] || "").trim(),
        password: String(values[rowIndex][passwordIdx] || "").trim()
      };
    }
  }

  return normalized === "admin" ? { username: "admin", password: "admin" } : null;
}

function normalizeAdminUsername_(value) {
  return String(value || "").trim();
}

function normalizeAdminPassword_(value) {
  return String(value || "").trim();
}

function uploadBase64ImageToDrive_(base64, fileName, mimeType, folderId) {
  try {
    const folder = DriveApp.getFolderById(folderId);
    const cleanBase64 = String(base64 || "").replace(/^data:.*;base64,/, "");
    const bytes = Utilities.base64Decode(cleanBase64);
    const blob = Utilities.newBlob(bytes, mimeType, fileName);
    const file = folder.createFile(blob);
    trySetPublicView_(file);
    return buildDriveImageUrl_(file.getId());
  } catch (error) {
    const detail = error && error.message ? error.message : String(error);
    Logger.log("Drive upload failed: " + detail);
    throw new Error("Upload poster ke Drive gagal: " + detail);
  }
}

function buildDriveImageUrl_(fileId) {
  const id = String(fileId || "").trim();
  if (!id) return "";
  return "https://drive.google.com/thumbnail?id=" + id + "&sz=w2000";
}

function uploadBase64FileToDrive_(base64, fileName, mimeType, folderId) {
  try {
    const folder = DriveApp.getFolderById(folderId);
    const cleanBase64 = String(base64 || "").replace(/^data:.*;base64,/, "");
    const bytes = Utilities.base64Decode(cleanBase64);
    const blob = Utilities.newBlob(bytes, mimeType, fileName);
    const file = folder.createFile(blob);
    trySetPublicView_(file);
    return "https://drive.google.com/uc?export=view&id=" + file.getId();
  } catch (error) {
    Logger.log("Drive file upload failed: " + (error && error.message ? error.message : String(error)));
    throw new Error("Upload trailer ke Drive gagal.");
  }
}

function trySetPublicView_(file) {
  try {
    file.setSharing(DriveApp.Access.ANYONE_WITH_LINK, DriveApp.Permission.VIEW);
  } catch (error) {
    Logger.log("Drive sharing skipped: " + (error && error.message ? error.message : String(error)));
  }
}

function getSheetHeaderMap_(headersRow) {
  const col = {};
  headersRow.forEach(function(header, index) {
    col[String(header || "").trim()] = index;
  });
  return col;
}

function getRowValueByHeader_(row, col, headerName) {
  if (!Object.prototype.hasOwnProperty.call(col, headerName)) return "";
  return row[col[headerName]];
}

function ensureSheetWithHeaders_(ss, sheetName, headers) {
  const sheet = ss.getSheetByName(sheetName) || ss.insertSheet(sheetName);
  if (sheet.getLastRow() === 0) {
    sheet.appendRow(headers);
  } else {
    const currentHeaders = sheet.getRange(1, 1, 1, headers.length).getValues()[0];
    const mismatch = headers.some(function(header, index) {
      return String(currentHeaders[index] || "").trim() !== header;
    });
    if (mismatch) {
      sheet.getRange(1, 1, 1, headers.length).setValues([headers]);
    }
  }
  return sheet;
}

function ensureAdminSheetWithDefaults_(ss) {
  const sheet = ensureSheetWithHeaders_(ss, SHEET_ADMIN, ADMIN_HEADERS);
  if (sheet.getLastRow() <= 1) {
    const values = sheet.getDataRange().getValues();
    const hasDefaultAdmin = values.length > 1 && values.slice(1).some(function(row) {
      return String(row[0] || "").trim().toLowerCase() === "admin";
    });
    if (!hasDefaultAdmin) {
      // Kredensial admin harus dibuat manual oleh pemilik Sheet.
    }
  }
  return sheet;
}

function readSheetObjects_(sheet) {
  const values = sheet.getDataRange().getValues();
  if (values.length <= 1) return [];
  const headers = values[0].map(function(header) { return String(header).trim(); });
  return values.slice(1).map(function(row) {
    const item = {};
    headers.forEach(function(header, index) { item[header] = row[index]; });
    return item;
  });
}

function parseDateValue_(value) {
  if (!value) return 0;
  if (Object.prototype.toString.call(value) === "[object Date]") return value.getTime();
  const parsed = new Date(value);
  return isNaN(parsed.getTime()) ? 0 : parsed.getTime();
}

function formatDateValue_(value) {
  const timestamp = parseDateValue_(value);
  if (!timestamp) return "";
  return Utilities.formatDate(new Date(timestamp), "Asia/Jakarta", "yyyy-MM-dd'T'HH:mm:ssXXX");
}

function jsonOutput_(payload) {
  return ContentService
    .createTextOutput(JSON.stringify(payload))
    .setMimeType(ContentService.MimeType.JSON);
}

function incrementReportCount_(sheetName, idHeader, targetId, countHeader) {
  const ss = SpreadsheetApp.openById(SPREADSHEET_ID);
  const sheet = ss.getSheetByName(sheetName);
  if (!sheet) return;
  const values = sheet.getDataRange().getValues();
  if (values.length <= 1) return;
  const col = getSheetHeaderMap_(values[0]);
  const idIdx = col[idHeader];
  const countIdx = col[countHeader];
  if (idIdx === undefined || countIdx === undefined) return;

  for (let r = 1; r < values.length; r++) {
    const rowId = String(values[r][idIdx] || "").trim();
    if (rowId === targetId) {
      const current = Number(values[r][countIdx] || 0);
      sheet.getRange(r + 1, countIdx + 1).setValue(current + 1);
      break;
    }
  }
}












