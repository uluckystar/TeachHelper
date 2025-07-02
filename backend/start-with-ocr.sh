#!/bin/bash

# TeachHelper后端启动脚本 - 包含OCR支持
# 使用方法: ./start-with-ocr.sh

echo "🚀 启动 TeachHelper 后端服务 (包含OCR支持)"

# 检查是否在正确的目录
if [ ! -f "pom.xml" ]; then
    echo "❌ 错误: 请在backend目录下运行此脚本"
    exit 1
fi

# 检查Tesseract是否安装
if ! command -v tesseract &> /dev/null; then
    echo "⚠️  警告: 未找到tesseract命令，OCR功能可能不可用"
    echo "请运行: brew install tesseract tesseract-lang"
fi

# 查找Tesseract库路径
TESSERACT_LIB_PATH=""
POSSIBLE_PATHS=(
    "/opt/homebrew/Cellar/tesseract/5.5.0_1/lib"
    "/opt/homebrew/lib"
    "/usr/local/lib"
    "/usr/lib"
)

for path in "${POSSIBLE_PATHS[@]}"; do
    if [ -f "$path/libtesseract.dylib" ] || [ -f "$path/libtesseract.5.dylib" ]; then
        TESSERACT_LIB_PATH="$path"
        echo "🔍 找到Tesseract库路径: $TESSERACT_LIB_PATH"
        break
    fi
done

if [ -z "$TESSERACT_LIB_PATH" ]; then
    echo "⚠️  警告: 未找到Tesseract库文件，OCR功能可能不可用"
    echo "请确保已正确安装Tesseract"
fi

# 设置JVM参数
JVM_ARGS="-Xmx2g"
if [ -n "$TESSERACT_LIB_PATH" ]; then
    JVM_ARGS="$JVM_ARGS -Djna.library.path=$TESSERACT_LIB_PATH -Djava.library.path=$TESSERACT_LIB_PATH"
fi

echo "📚 JVM参数: $JVM_ARGS"

# 停止现有进程
echo "🛑 停止现有服务进程..."
pkill -f spring-boot 2>/dev/null || true
sleep 2

# 启动服务
echo "🚀 启动服务..."
mvn spring-boot:run -Dspring-boot.run.jvmArguments="$JVM_ARGS" 