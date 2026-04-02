type LogLevel = 'debug' | 'info' | 'warn' | 'error';

const LEVEL_PRIORITY: Record<LogLevel, number> = {
  debug: 0,
  info: 1,
  warn: 2,
  error: 3,
};

const IS_PRODUCTION = import.meta.env.PROD;
const MIN_LEVEL: LogLevel = IS_PRODUCTION ? 'info' : 'debug';

function shouldLog(level: LogLevel): boolean {
  return LEVEL_PRIORITY[level] >= LEVEL_PRIORITY[MIN_LEVEL];
}

function formatMessage(level: LogLevel, context: string, message: string): string {
  return `[${new Date().toISOString()}] [${level.toUpperCase()}] [${context}] ${message}`;
}

export const logger = {
  debug(context: string, message: string) {
    if (shouldLog('debug')) {
      console.debug(formatMessage('debug', context, message));
    }
  },
  info(context: string, message: string) {
    if (shouldLog('info')) {
      console.info(formatMessage('info', context, message));
    }
  },
  warn(context: string, message: string) {
    if (shouldLog('warn')) {
      console.warn(formatMessage('warn', context, message));
    }
  },
  error(context: string, message: string) {
    if (shouldLog('error')) {
      console.error(formatMessage('error', context, message));
    }
  },
};
