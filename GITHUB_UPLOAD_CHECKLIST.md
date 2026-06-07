# GitHub Upload Checklist

## 推荐上传

这些内容属于 FoodCore 主项目本体，建议上传到 GitHub 仓库：

- `src/`
- `gradle/`
- `gradlew`
- `gradlew.bat`
- `build.gradle`
- `settings.gradle`
- `gradle.properties`
- `.gitattributes`
- `.gitignore`
- `.github/`
- `README.md`
- `MODRINTH_RELEASE_0.1.0.md`
- `libs/`

## 不要上传

这些内容属于本地运行产物、IDE 配置或外部源码副本，不建议上传：

- `build/`
- `.gradle/`
- `run/`
- `runs/`
- `run-data/`
- `.idea/`
- `.vscode/`
- `.eclipse/`
- `out/`
- `repo/`
- `Create-mc1.21.1-dev/`
- `Cold-Sweat-1.21-FG/`
- `Freeze-It-And-Heat-It-dev-NeoForge-1.21.1/`

## 为什么保留 libs

当前 `build.gradle` 使用的是本地文件依赖：

- `libs/ColdSweat-2.4.1.jar`
- `libs/create-1.21.1-6.0.10.jar`

如果不上传 `libs/`，别人克隆仓库后将无法直接构建。

## 仓库初始化建议

如果你准备新建 GitHub 仓库，建议仓库命名：

- `FoodCore-1.21.1-neoforge`

建议在 GitHub 创建空仓库后再上传以上文件。

## 发布文件说明

GitHub 源码仓库与 Modrinth 发布文件分开处理：

- GitHub 仓库：上传源码和构建脚本
- Modrinth 版本文件：上传 `build/libs/foodcore-0.1.0.jar`

不要把 `build/libs/foodcore-0.1.0.jar` 当作源码仓库常规文件提交。
