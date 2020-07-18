# Multi select GalleryPicker


# BottomSheet Image Picker for Android

A modern image picker implemented as [BottomSheet](https://developer.android.com/reference/android/support/design/widget/BottomSheetDialogFragment).

## Demo
![Alt Text](https://media.giphy.com/media/Sv9JIkuxnqjRMKh9ot/giphy.gif)



## Features

1. select single/multiple images or videos right in the bottom sheet
2. use camera to take a picture
3. handles all permission requests

This library is based on [bottomsheet-imagepicker](https://github.com/kroegerama/bottomsheet-imagepicker).
Removed camera and gallery tiles and added support of video

## How to Use

Minimum SDK: 21

### Add to Project

First make sure `jitpack` is included as a repository in your **project**'s build.gradle:  

```groovy
allprojects {
    repositories {
        //...
        maven { url 'https://jitpack.io' }
    }
}
```

And then add the below to your app's build.gradle:  

```groovy
    implementation 'implementation 'com.github.timkabot:GalleryPicker:<version>'
```


### Step 1: Implement the callback handler

The caller Activity or Fragment has to implement `BottomSheetImagePicker.OnImagesSelectedListener` to receive the selection callbacks. It will automatically be used by the image picker. No need to register a listener.

##### Kotlin

```kotlin
class AcMain: BaseActivity(), BottomSheetGalleryPicker.OnImagesSelectedListener {
    //...

    override fun onImagesSelected(uris: List<Uri>, tag: String?) {
        // Do something with selected files
    }
}
```

### Step 2: Create the image picker using the Builder

The setters are all **optional** and the builder will fallback to default values.

#### single select
##### Kotlin

```kotlin
          BottomSheetGalleryPicker
                .Builder()
                .peekHeight(R.dimen.peekHeight)     //peek height of the bottom sheet
                .columnSize(R.dimen.columnSize)     //size of the columns (will be changed a little to fit)
                .selectionRange(1, 6)
                .selectionTitles(
                    R.plurals.imagePickerMulti,           //"you have selected <count> images
                    R.plurals.imagePickerMultiMore,      //"You have to select <min-count> more images"
                    R.string.imagePickerMultiLimit       //"You cannot select more than <max> images"
                )
                .show(supportFragmentManager)
```

### The image picker works in activities and fragments
##### Kotlin
```kotlin
    //inside activity
        .show(supportFragmentManager)
    //inside fragment
        .show(childFragmentManager)
```
```
