import { randomUUID } from 'node:crypto';
import express from 'express';
import { StreamableHTTPServerTransport } from '@modelcontextprotocol/sdk/server/streamableHttp.js';
import { BankingApiClient } from './client/banking-api.js';
import { loadConfig } from './config.js';
import { createMcpServer } from './server.js';

async function main() {
  const config = loadConfig();
  const client = new BankingApiClient({
    baseUrl: config.apiBaseUrl,
    tenantId: config.tenantId,
    userId: config.userId,
    apiKey: config.apiKey,
  });

  const mcpServer = createMcpServer(client);
  const transport = new StreamableHTTPServerTransport({ sessionIdGenerator: () => randomUUID() });
  await mcpServer.connect(transport);

  const app = express();
  app.use(express.json());

  app.get('/health', (_req, res) => res.json({ status: 'UP', service: 'banking-forms-mcp' }));

  app.use((req, res, next) => {
    if (req.path === '/health') return next();
    if (!config.apiKey) return next();
    const auth = req.headers.authorization;
    if (auth !== `Bearer ${config.apiKey}`) {
      return res.status(401).json({ error: 'Unauthorized' });
    }
    next();
  });

  app.all('/mcp', async (req, res) => {
    await transport.handleRequest(req, res, req.body);
  });

  app.listen(config.httpPort, () => {
    console.error(`Banking Forms MCP server listening on http://localhost:${config.httpPort}/mcp`);
  });
}

main().catch((err) => {
  console.error('MCP HTTP server failed:', err);
  process.exit(1);
});
