# Minecraft Mod Generator — generator-core (NeoForge 1.21.x 文档解析层)

Java 21 + Spring Boot 3 模组生成器项目的核心解析库。本阶段实现了 **generator-core**
中 NeoForge 1.21.x 的文档解析层：爬取 neoforged/Documentation 官方文档，用 commonmark
解析 Markdown，用 QDox 解析 NeoForge 源码，输出结构化 `ApiIndex`，为后续
CodeGenerator（FreeMarker 生成模组工程）与 BuildValidator（gradlew 构建验证）提供数据。

> 整个解析过程不依赖任何 AI 推理：爬取 → 解析 → 合并 → 校验均为确定性规则。

## 模块结构

```
minecraft-mod-generator/
├── settings.gradle                     # Gradle 多模块（include 'generator-core'）
├── generator-core/
│   ├── build.gradle                    # java-library，Java 21 toolchain
│   └── src/
│       ├── main/java/com/example/modgen/core/
│       │   ├── model/                  # ApiIndex, ModuleIndex, ApiClass, ApiMethod, ...
│       │   ├── source/                 # DocumentationSource(sealed) → GitHubApiSource | LocalDirectorySource
│       │   ├── parse/markdown/         # MarkdownApiExtractor (commonmark)
│       │   ├── parse/java/             # JavaApiExtractor (QDox)
│       │   └── parse/neoforge/         # NeoForgeDocumentationParser（五阶段编排）等
│       └── test/
│           ├── java/...                # 18 个单元测试（Markdown/Java/NeoForge 三层）
│           └── resources/fixtures/     # 文档与源码夹具
```

## 构建与测试

要求：JDK 21（gradle toolchain 自动使用），Gradle 8.x。

```bash
export JAVA_HOME=<JDK 21 路径>
gradle test        # 运行全部单元测试（18/18）
gradle jar         # 产出 generator-core/build/libs/generator-core-0.1.0-SNAPSHOT.jar
```

## 五阶段解析管线

1. **CRAWL** — 按版本目录前缀（如 `versioned_docs/version-1.21.1`）列出 Markdown 文件，
   `ModuleMapper` 将相对路径映射到模块（blocks/items/registration/...），按请求过滤。
2. **MARKDOWN** — commonmark 逐文件提取代码块、API 引用、表格、admonition 笔记、
   配置项与默认值（虚拟线程池并行）。
3. **JAVA_SOURCE** — QDox 从 NeoForge 源码（maven.neoforged.net 的 sources jar）
   提取 `DeferredRegister(.Blocks/.Items)`、`RegistryObject`、`DeferredHolder`、
   `DeferredBlock`、`DeferredItem`、`RegisterEvent` 的签名与 Javadoc。
4. **MERGE** — 按模块分组；API 引用解析顺序：QDox 解析类 → `KnownApiDictionary` 内置词典。
5. **VALIDATION** — 请求模块缺失或关键注册类缺失即抛 `ParsingException`。

硬依赖规则：仅请求 blocks/items 时自动纳入 registration 模块。

## 真实数据验证

- 单元测试 18/18 通过。
- 端到端冒烟（真实 neoforged/Documentation 仓库 + 真实 NeoForge 21.1.252 sources jar）：
  8 个文档文件 → 3 个模块；真实 `DeferredRegister.Blocks` 方法签名（含
  `<B extends Block>` 边界）、配置项默认值（`sound=SoundType.STONE`、`friction=0.6`）
  提取成功。

## 已知限制与说明

- `GitHubApiSource` 需要 User-Agent 头；未认证 API 限流 60 次/时，生产环境应注入
  带 token 的 HttpClient（或使用 codeload tarball + `LocalDirectorySource`）。
- NeoForge sources jar 不在 Maven Central，仓库为 `https://maven.neoforged.net/releases`。
- `RegistryObject` 在 21.1.252 源码中已不存在（迁移至 `DeferredHolder`），解析器
  WARN + 跳过；夹具保留它以覆盖仍有该类的版本。
- 部分配置项（destroyTime/explosionResistance/lightLevel）默认值为 `null`：1.21.1
  文档未给出显式默认值，CodeGenerator 阶段可用内置词典补齐。

## 下一步

- CodeGenerator：FreeMarker 按 `ApiIndex` 生成可编译 Gradle 模组工程。
- BuildValidator：ProcessBuilder 调用 `gradlew build` 捕获编译错误。
- generator-web：Spring Boot 3 REST API，按用户选择（加载器/版本/功能）生成 ZIP。
