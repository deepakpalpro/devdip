export interface StatusMeta {
  label: string;
  className: string;
  description: string;
}

const META: Record<string, StatusMeta> = {
  DRAFT: { label: 'Draft', className: 'bf-badge', description: 'Not submitted yet — you can continue where you left off.' },
  SUBMITTED: { label: 'Submitted', className: 'bf-badge', description: 'Received and queued for processing.' },
  VALIDATING: { label: 'Validating', className: 'bf-badge', description: 'We are checking your answers.' },
  PROCESSING: { label: 'Processing', className: 'bf-badge', description: 'Your application is being processed.' },
  PENDING_REVIEW: { label: 'In review', className: 'bf-badge', description: 'A specialist is reviewing your application.' },
  APPROVED: { label: 'Approved', className: 'bf-badge bf-badge-success', description: 'Your application has been approved.' },
  REJECTED: { label: 'Rejected', className: 'bf-badge bf-badge-danger', description: 'Unfortunately this application was not approved.' },
  NEEDS_INFO: {
    label: 'Action needed',
    className: 'bf-badge bf-badge-warning',
    description: 'We need more information before we can continue.',
  },
};

export function statusMeta(status: string): StatusMeta {
  return META[status] ?? { label: status, className: 'bf-badge', description: '' };
}

export function isDraft(status: string): boolean {
  return status === 'DRAFT';
}
