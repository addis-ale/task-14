import dayjs from "dayjs";

export function formatDateTime(value?: string | number | Date | null): string {
  if (!value) {
    return "-";
  }
  return dayjs(value).format("YYYY-MM-DD HH:mm");
}

export function formatDate(value?: string | number | Date | null): string {
  if (!value) {
    return "-";
  }
  return dayjs(value).format("YYYY-MM-DD");
}

export function fromNow(value?: string | number | Date | null): string {
  if (!value) {
    return "-";
  }
  const minute = dayjs().diff(dayjs(value), "minute");
  if (minute <= 1) {
    return "just now";
  }
  return `${minute} min ago`;
}
