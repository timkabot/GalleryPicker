package kz.timkabor.bottomsheetgallerypickerlibrary

import android.provider.MediaStore

const val TAG = "BottomSheetImagePicker"

const val LOADER_ID = 0x1337

const val REQUEST_PERMISSION_READ_STORAGE = 0x2000
const val REQUEST_PERMISSION_WRITE_STORAGE = 0x2001

const val REQUEST_PHOTO = 0x3000

const val KEY_MULTI_SELECT_MIN = "multiSelectMin"
const val KEY_MULTI_SELECT_MAX = "multiSelectMax"
const val KEY_COLUMN_SIZE_RES = "columnCount"

const val KEY_TITLE_RES_MULTI = "titleResMulti"
const val KEY_TITLE_RES_MULTI_MORE = "titleResMultiMore"
const val KEY_TITLE_RES_MULTI_LIMIT = "titleResMultiLimit"
const val STATE_SELECTION = "stateSelection"
const val STATE_CURRENT_URI = "stateUri"


const val KEY_PEEK_HEIGHT = "peekHeight"


const val MAX_CURSOR_IMAGES = 512

const val VIDEO_AND_IMAGE_SELECTION = (MediaStore.Files.FileColumns.MEDIA_TYPE + "="
        + MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE
        + " OR "
        + MediaStore.Files.FileColumns.MEDIA_TYPE + "="
        + MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO)