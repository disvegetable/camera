# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html.
# For more examples on how to use CMake, see https://github.com/android/ndk-samples.

# Sets the minimum CMake version required for this project.
cmake_minimum_required(VERSION 3.22.1)

project("opencv_app")

#add native-lib
add_library(${CMAKE_PROJECT_NAME} SHARED
        # List C/C++ source files with relative paths to this CMakeLists.txt.
        native-lib.cpp)

#include opencv head files
include_directories(${CMAKE_SOURCE_DIR}/include)
add_library(libopencv_java4 SHARED IMPORTED)

#set .so
set_target_properties(libopencv_java4 PROPERTIES IMPORTED_LOCATION ${CMAKE_SOURCE_DIR}/jniLibs//${ANDROID_ABI}/libopencv_java4.so)
set(ncnn_DIR ${CMAKE_SOURCE_DIR}/include/ncnn-20240410-android-vulkan/${ANDROID_ABI}/lib/cmake/ncnn)
find_package(ncnn REQUIRED)
# Specifies libraries CMake should link to your target library. You
# can link libraries from various origins, such as libraries defined in this
# build script, prebuilt third-party libraries, or Android system libraries.
target_link_libraries(${CMAKE_PROJECT_NAME}
        # List libraries link to the target library
        libopencv_java4
        android
        log)