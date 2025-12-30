# Top Hog Web 使用与发布流程

本文档详细说明了 `top_hog_web` 前端项目的初始化、开发、打包及发布流程。

## 1. 环境准备

确保本地开发环境已安装以下工具：
- [Node.js](https://nodejs.org/) (建议使用 LTS 版本)
- npm (Node.js 自带)

## 2. 项目初始化 (Initialization)

在首次获取代码后，需要安装项目依赖。在 `top_hog_web` 目录下执行：

```bash
npm install
```

此命令会根据 `package.json` 下载并安装所需的依赖包到 `node_modules` 目录。

## 3. 开发环境使用 (Usage)

在本地开发调试时，使用以下命令启动开发服务器：

```bash
npm run dev
```

启动成功后，终端会显示访问地址，通常为：
`http://localhost:5173/`

开发服务器支持热重载（Hot Module Replacement），修改代码后浏览器会自动刷新。

## 4. 打包构建 (Packaging)

当项目开发完成并准备发布时，需要构建生产环境代码。执行以下命令：

```bash
npm run build
```

**说明：**
- 该命令会执行 `vite build`。
- 构建生成的文件将存放在项目根目录下的 `dist` 文件夹中。
- `dist` 文件夹包含了经过压缩和优化的 HTML、CSS、JavaScript 和静态资源文件。

## 5. 本地预览 (Preview)

在正式部署之前，可以在本地预览打包后的 `dist` 文件运行效果：

```bash
npm run preview
```

此命令会启动一个本地服务器来托管 `dist` 目录，用于验证构建产物是否正常工作。

## 6. 发布部署 (Deployment)

发布流程即是将打包生成的 `dist` 目录部署到 Web 服务器上。

### 部署步骤：
1. 执行 `npm run build` 生成 `dist` 目录。
2. 将 `dist` 目录下的所有内容上传到服务器的 Web 根目录（例如 Nginx 的 html 目录）。
3. 配置 Web 服务器（如 Nginx）以支持 Vue Router 的 History 模式（如果使用了的话）和 API 代理。

### Nginx 配置示例：

```nginx
server {
    listen 80;
    server_name your-domain.com;

    # 指向 dist 目录的路径
    root /path/to/top_hog_web/dist;
    index index.html;

    # 处理前端路由 (防止刷新 404)
    location / {
        try_files $uri $uri/ /index.html;
    }

    # 代理 API 请求到后端 (假设后端在 8088 端口)
    location /api {
        proxy_pass http://localhost:8088;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```
