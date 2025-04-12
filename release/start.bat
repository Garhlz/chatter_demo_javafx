@echo off

:: 设置当前目录为脚本所在目录
cd /d "%~dp0"

:: 设置服务器和客户端映像的启动器路径（相对路径，无需跳目录）
set "SERVER_LAUNCHER=chat-server-image\bin\chat-server.bat"
set "CLIENT_LAUNCHER=chat-client-image\bin\chat-client.bat"

:: 检查服务器映像是否存在
if not exist "%SERVER_LAUNCHER%" (
    echo Error: Server launcher not found at "%SERVER_LAUNCHER%"
    exit /b 1
)

:: 检查客户端映像是否存在
if not exist "%CLIENT_LAUNCHER%" (
    echo Error: Client launcher not found at "%CLIENT_LAUNCHER%"
    exit /b 1
)

:: 启动服务器进程
echo Starting server...
start "" "%SERVER_LAUNCHER%"
timeout /t 2 >nul

:: 启动三个客户端进程
echo Starting client 1...
start "" "%CLIENT_LAUNCHER%"

echo Starting client 2...
start "" "%CLIENT_LAUNCHER%"

echo Starting client 3...
start "" "%CLIENT_LAUNCHER%"

echo All processes started.
