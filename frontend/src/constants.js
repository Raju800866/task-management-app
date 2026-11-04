export const STATUSES = [
  { value: 'TODO', label: 'To do', badge: 'bg-slate-100 text-slate-700' },
  { value: 'IN_PROGRESS', label: 'In progress', badge: 'bg-amber-100 text-amber-800' },
  { value: 'DONE', label: 'Done', badge: 'bg-emerald-100 text-emerald-800' },
]

export const PRIORITIES = [
  { value: 'LOW', label: 'Low', badge: 'bg-sky-50 text-sky-700 ring-sky-200' },
  { value: 'MEDIUM', label: 'Medium', badge: 'bg-violet-50 text-violet-700 ring-violet-200' },
  { value: 'HIGH', label: 'High', badge: 'bg-rose-50 text-rose-700 ring-rose-200' },
]

export const statusOf = (v) => STATUSES.find((s) => s.value === v) ?? STATUSES[0]
export const priorityOf = (v) => PRIORITIES.find((p) => p.value === v) ?? PRIORITIES[1]
