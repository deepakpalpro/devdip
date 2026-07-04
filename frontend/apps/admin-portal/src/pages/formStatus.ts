export function statusBadgeClass(status: string): string {
  switch (status) {
    case 'PUBLISHED':
      return 'bf-badge bf-badge-success';
    case 'DEPRECATED':
      return 'bf-badge bf-badge-danger';
    case 'DRAFT':
    default:
      return 'bf-badge bf-badge-warning';
  }
}
