@echo off
echo 正在编译贪吃蛇游戏...
javac -encoding UTF-8 -d bin src/*.java
if %errorlevel% equ 0 (
    echo 编译成功！
    echo 正在启动游戏...
    java -cp bin SnakeGame
) else (
    echo 编译失败！
    pause
)