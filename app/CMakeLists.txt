cmake_minimum_required(VERSION 3.22.1)

project("termux_ultra")

# Configure build for different architectures
set(CMAKE_ANDROID_ARCH_ABI ${ANDROID_ABI})
set(CMAKE_ANDROID_STL_TYPE c++_shared)

# Add Rust library path
set(RUST_LIB_DIR ${CMAKE_SOURCE_DIR}/../rust-core/target/${ANDROID_ABI}/release)

# Include directories
include_directories(
    ${CMAKE_SOURCE_DIR}/src/main/cpp
    ${CMAKE_SOURCE_DIR}/../rust-core/src
)

# Add native source files
file(GLOB_RECURSE NATIVE_SRCS
    "src/main/cpp/*.cpp"
    "src/main/cpp/*.c"
)

# Create the main library
add_library(bifrost SHARED ${NATIVE_SRCS})

# Add Rust library as imported
add_library(termux_ultra_bifrost SHARED IMPORTED)
set_target_properties(termux_ultra_bifrost PROPERTIES
    IMPORTED_LOCATION ${RUST_LIB_DIR}/libtermux_ultra_bifrost.so
)

# Link libraries
target_link_libraries(bifrost
    android
    log
    termux_ultra_bifrost
)

# Create JNI wrapper libraries
add_library(llama_jni SHARED
    src/main/cpp/llama_jni.cpp
)

add_library(mlc4j SHARED
    src/main/cpp/mlc4j.cpp
)

# Link JNI libraries
target_link_libraries(llama_jni
    android
    log
    bifrost
)

target_link_libraries(mlc4j
    android
    log
    bifrost
)

# Add compiler flags
target_compile_options(bifrost PRIVATE
    -Wall
    -Wextra
    -O2
    -fvisibility=hidden
)

# Export symbols for JNI
set_target_properties(bifrost PROPERTIES
    LINK_FLAGS "-Wl,--version-script=${CMAKE_SOURCE_DIR}/src/main/cpp/exports.map"
)