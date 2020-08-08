# Multi select GalleryPicker


# BottomSheet Image Picker for Android

A modern image picker implemented as [BottomSheet](https://developer.android.com/reference/android/support/design/widget/BottomSheetDialogFragment).
Project must be cimoatble with java 1.8
```
    compileOptions {
        sourceCompatibility 1.8
        targetCompatibility 1.8
    }
    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_1_8.toString()
    }
```
## Demo
![Alt Text](https://media.giphy.com/media/Sv9JIkuxnqjRMKh9ot/giphy.gif)



## Features

1. select single/multiple images or videos right in the bottom sheet
2. use camera with camera preview to take a picture 
3. handles all permission requests

This library is based on [bottomsheet-imagepicker](https://github.com/kroegerama/bottomsheet-imagepicker).
Changed design:
    Removed camera and gallery tiles
    Added support of video, also added camera preview

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

```

### Create the image picker using the Builder

The setters are all **optional** and the builder will fallback to default values.

##### Kotlin

```kotlin
       BottomSheetGalleryPicker
                .Builder(onImagesSelectedListener)
                .peekHeight(R.dimen.peekHeight)
                .columnSize(R.dimen.columnSize)
                .selectionRange(1, 6)
                .selectionTitles(
                    R.plurals.imagePickerMulti,           //"you have selected <count> images
                    R.string.imagePickerMultiLimit       //"You cannot select more than <max> images"
                )
                .show(supportFragmentManager)
                
                
                   
      private val onImagesSelectedListener =
        object : BottomSheetGalleryPicker.OnImagesSelectedListener {
            override fun onImagesSelected(uris: List<Uri>, fromCamera: Boolean) {
                if( fromCamera) 
                {  do somethng with camera  }
                else { handle chosen files }
            }
        }
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
