cmake_minimum_required(VERSION 3.22.1)
project(edge_native)

add_library(
    edge_native
    SHARED
    native-lib.cpp
    edge_processor.cpp
)

find_library(log-lib log)

# Optionally set OpenCV_DIR and enable
find_package(OpenCV QUIET)

find_library(GLESv2-lib GLESv2)
find_library(EGL-lib EGL)

target_link_libraries(
    edge_native
    ${OpenCV_LIBS}
    ${GLESv2-lib}
    ${EGL-lib}
    ${log-lib}
)


