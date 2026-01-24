#!/bin/bash

# Скрипт для сборки SQLCipher с ICU поддержкой для Android

set -e

echo "🔧 Building SQLCipher with ICU support for Android..."

# Создаем директорию для сборки
BUILD_DIR="$(pwd)/sqlcipher-icu-build"
mkdir -p "$BUILD_DIR"
cd "$BUILD_DIR"

# Скачиваем исходники SQLCipher
if [ ! -d "sqlcipher" ]; then
    echo "📦 Downloading SQLCipher source..."
    git clone https://github.com/sqlcipher/sqlcipher.git
fi

cd sqlcipher

# Скачиваем ICU
if [ ! -d "icu" ]; then
    echo "📦 Downloading ICU source..."
    wget https://github.com/unicode-org/icu/releases/download/release-73-2/icu4c-73_2-src.tgz
    tar -xzf icu4c-73_2-src.tgz
    mv icu icu-source
fi

# Настраиваем Android NDK
export ANDROID_NDK_ROOT="$ANDROID_HOME/ndk/25.1.8937393"  # Adjust version as needed
export TOOLCHAIN="$ANDROID_NDK_ROOT/toolchains/llvm/prebuilt/linux-x86_64"

# Функция для сборки под конкретную архитектуру
build_for_arch() {
    local ARCH=$1
    local API_LEVEL=21
    
    echo "🏗️ Building for $ARCH..."
    
    case $ARCH in
        "arm64-v8a")
            export CC="$TOOLCHAIN/bin/aarch64-linux-android${API_LEVEL}-clang"
            export CXX="$TOOLCHAIN/bin/aarch64-linux-android${API_LEVEL}-clang++"
            export AR="$TOOLCHAIN/bin/llvm-ar"
            export STRIP="$TOOLCHAIN/bin/llvm-strip"
            ;;
        "armeabi-v7a")
            export CC="$TOOLCHAIN/bin/armv7a-linux-androideabi${API_LEVEL}-clang"
            export CXX="$TOOLCHAIN/bin/armv7a-linux-androideabi${API_LEVEL}-clang++"
            export AR="$TOOLCHAIN/bin/llvm-ar"
            export STRIP="$TOOLCHAIN/bin/llvm-strip"
            ;;
        "x86_64")
            export CC="$TOOLCHAIN/bin/x86_64-linux-android${API_LEVEL}-clang"
            export CXX="$TOOLCHAIN/bin/x86_64-linux-android${API_LEVEL}-clang++"
            export AR="$TOOLCHAIN/bin/llvm-ar"
            export STRIP="$TOOLCHAIN/bin/llvm-strip"
            ;;
    esac
    
    # Сборка ICU для данной архитектуры
    cd icu-source/source
    ./configure --host=${CC%-*} --enable-static --disable-shared --with-data-packaging=static
    make -j$(nproc)
    cd ../..
    
    # Конфигурируем SQLCipher с ICU
    ./configure \
        --host=${CC%-*} \
        --enable-tempstore=yes \
        --enable-threadsafe=yes \
        --enable-cross-thread-connections \
        --disable-tcl \
        --enable-shared=no \
        --enable-static=yes \
        CFLAGS="-DSQLITE_HAS_CODEC -DSQLITE_ENABLE_ICU -I$(pwd)/icu-source/source/common -I$(pwd)/icu-source/source/i18n" \
        LDFLAGS="-L$(pwd)/icu-source/source/lib -licuuc -licui18n -licudata"
    
    # Собираем
    make -j$(nproc)
    
    # Копируем результат
    mkdir -p "../libs/$ARCH"
    cp .libs/libsqlcipher.a "../libs/$ARCH/"
    
    # Очищаем для следующей архитектуры
    make clean
    cd icu-source/source && make clean && cd ../..
}

# Собираем для всех архитектур
for arch in "arm64-v8a" "armeabi-v7a" "x86_64"; do
    build_for_arch "$arch"
done

echo "✅ SQLCipher with ICU built successfully!"
echo "📁 Libraries are in: $BUILD_DIR/libs/"