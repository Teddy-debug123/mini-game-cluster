@echo off
chcp 65001 >nul
cd /d "%~dp0"
echo ========================================
echo         贪吃蛇游戏启动器
echo ========================================
echo.
if exist "SnakeGame.jar" (
    echo [模式] 使用JAR文件启动
    echo.
    echo 正在启动游戏...
    echo [提示] 关闭窗口即可退出游戏
    echo.
    java -jar SnakeGame.jar
) else (
    echo [模式] 编译启动
    echo.
    echo 正在编译贪吃蛇游戏...
    javac -encoding UTF-8 -d bin src/*.java
    if %errorlevel% neq 0 (
        echo.
        echo [错误] 编译失败！请检查代码是否有语法错误
        echo.
        pause
        exit /b 1
    )
    echo [成功] 编译完成！
    echo.
    echo 正在启动游戏...
    echo [提示] 关闭窗口即可退出游戏
    echo.
    java -cp bin snakegame.SnakeGame
)
echo.
echo 游戏已退出
pause