import type { ExtractedEntity } from '../types.js';

const PATTERNS: Array<{ name: string; regex: RegExp; parse: (m: RegExpMatchArray) => string | number }> = [
  {
    name: 'firstName',
    regex: /\b(?:my name is|i am|i'm)\s+([A-Z][a-z]+)/i,
    parse: (m) => m[1],
  },
  {
    name: 'lastName',
    regex: /\b(?:last name|surname)\s+(?:is\s+)?([A-Z][a-z]+)/i,
    parse: (m) => m[1],
  },
  {
    name: 'fullName',
    regex: /\b(?:my name is|i am|i'm)\s+([A-Z][a-z]+)\s+([A-Z][a-z]+)/i,
    parse: (m) => `${m[1]} ${m[2]}`,
  },
  {
    name: 'amount',
    regex: /\$?\s*([\d,]+(?:\.\d{2})?)\s*(?:dollars|usd)?/i,
    parse: (m) => Number(m[1].replace(/,/g, '')),
  },
  {
    name: 'email',
    regex: /\b([a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,})\b/,
    parse: (m) => m[1],
  },
  {
    name: 'phone',
    regex: /\b(\+?\d{1,3}[-.\s]?)?\(?\d{3}\)?[-.\s]?\d{3}[-.\s]?\d{4}\b/,
    parse: (m) => m[0],
  },
  {
    name: 'accountType',
    regex: /\b(checking|savings)\b/i,
    parse: (m) => m[1].charAt(0).toUpperCase() + m[1].slice(1).toLowerCase(),
  },
  {
    name: 'city',
    regex: /\b(?:city|live in|located in)\s+(?:is\s+)?([A-Z][a-z]+(?:\s+[A-Z][a-z]+)?)/i,
    parse: (m) => m[1],
  },
  {
    name: 'postcode',
    regex: /\b(?:postcode|zip|postal code)\s+(?:is\s+)?(\d{4,6})\b/i,
    parse: (m) => m[1],
  },
];

export function extractEntities(text: string): ExtractedEntity[] {
  const entities: ExtractedEntity[] = [];
  const seen = new Set<string>();

  for (const pattern of PATTERNS) {
    const match = text.match(pattern.regex);
    if (match && !seen.has(pattern.name)) {
      seen.add(pattern.name);
      entities.push({
        name: pattern.name,
        value: pattern.parse(match),
        confidence: 0.75,
      });
    }
  }

  const fullName = entities.find((e) => e.name === 'fullName');
  if (fullName && typeof fullName.value === 'string') {
    const [first, ...rest] = fullName.value.split(' ');
    if (!seen.has('firstName')) {
      entities.push({ name: 'firstName', value: first, confidence: 0.8 });
    }
    if (!seen.has('lastName') && rest.length > 0) {
      entities.push({ name: 'lastName', value: rest.join(' '), confidence: 0.8 });
    }
  }

  return entities;
}
