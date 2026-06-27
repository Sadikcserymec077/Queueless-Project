export function formatDate(value) {
  if (!value) return "Not available";
  return new Intl.DateTimeFormat(undefined, {
    dateStyle: "medium",
    timeStyle: "short"
  }).format(new Date(value));
}

export function minutesLabel(value) {
  if (value === null || value === undefined) return "Not available";
  if (value < 1) return "Now";
  if (value === 1) return "1 minute";
  return `${value} minutes`;
}

export function apiError(error) {
  return error?.response?.data?.message || error?.message || "Something went wrong";
}
