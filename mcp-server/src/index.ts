#!/usr/bin/env node
import { StdioServerTransport } from '@modelcontextprotocol/sdk/server/stdio.js';
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

  const server = createMcpServer(client);
  const transport = new StdioServerTransport();
  await server.connect(transport);
}

main().catch((err) => {
  console.error('MCP server failed:', err);
  process.exit(1);
});
