import type { FormSuggestion, FormSummary } from '../types.js';

const INTENT_KEYWORDS: Record<string, string[]> = {
  LOAN_APPLICATION: ['loan', 'borrow', 'lending', 'personal loan', 'credit', 'mortgage'],
  ACCOUNT_OPENING: ['account', 'open account', 'checking', 'savings', 'deposit', 'new account'],
};

export function suggestForms(userIntent: string, catalog: FormSummary[]): FormSuggestion[] {
  const normalized = userIntent.toLowerCase();
  const scored: FormSuggestion[] = [];

  for (const form of catalog) {
    let score = 0;
    const reasons: string[] = [];

    const keywords = INTENT_KEYWORDS[form.code] ?? [];
    for (const kw of keywords) {
      if (normalized.includes(kw)) {
        score += kw.split(' ').length > 1 ? 3 : 2;
        reasons.push(`matched "${kw}"`);
      }
    }

    if (form.name && normalized.includes(form.name.toLowerCase())) {
      score += 4;
      reasons.push('matched form name');
    }
    if (form.category && normalized.includes(form.category.toLowerCase())) {
      score += 2;
      reasons.push('matched category');
    }
    if (normalized.includes(form.code.toLowerCase().replace(/_/g, ' '))) {
      score += 3;
      reasons.push('matched form code');
    }

    if (score > 0) {
      scored.push({
        formCode: form.code,
        formName: form.name,
        category: form.category,
        score,
        reason: reasons.join('; ') || 'keyword match',
      });
    }
  }

  return scored.sort((a, b) => b.score - a.score);
}

export function topSuggestionOrNull(suggestions: FormSuggestion[]): FormSuggestion | null {
  return suggestions.length > 0 ? suggestions[0] : null;
}
