export function maskStudentId(
  value: string | number | undefined | null,
  showRaw = false,
): string {
  if (value === undefined || value === null || value === "") {
    return "-";
  }

  const raw = String(value);
  if (showRaw || raw.length <= 4) {
    return raw;
  }

  return `${"*".repeat(Math.max(0, raw.length - 4))}${raw.slice(-4)}`;
}
