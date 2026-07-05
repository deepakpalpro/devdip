export interface ServerConfig {
  apiBaseUrl: string;
  tenantId: string;
  userId: string;
  apiKey: string | undefined;
  httpPort: number;
  transport: 'stdio' | 'http';
}

export function loadConfig(): ServerConfig {
  return {
    apiBaseUrl: process.env.BANKING_API_URL ?? 'http://localhost:8080',
    tenantId: process.env.BANKING_TENANT_ID ?? '11111111-1111-1111-1111-111111111111',
    userId: process.env.BANKING_USER_ID ?? '44444444-4444-4444-4444-444444444444',
    apiKey: process.env.MCP_API_KEY,
    httpPort: Number(process.env.MCP_HTTP_PORT ?? 3100),
    transport: (process.env.MCP_TRANSPORT ?? 'stdio') as 'stdio' | 'http',
  };
}
