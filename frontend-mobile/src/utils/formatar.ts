export function dataCurta(iso: string | null | undefined): string {
  if (!iso) return 'Data não informada';
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return 'Data não informada';
  return d.toLocaleDateString('pt-BR', { day: '2-digit', month: '2-digit', year: 'numeric' });
}
