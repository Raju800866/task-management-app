import { STATUSES, categoryOf, priorityOf } from '../constants.js'

function isOverdue(task) {
  if (!task.dueDate || task.status === 'DONE') return false
  const today = new Date().toISOString().slice(0, 10)
  return task.dueDate < today
}

function formatDate(iso) {
  return new Date(`${iso}T00:00:00`).toLocaleDateString(undefined, { day: 'numeric', month: 'short', year: 'numeric' })
}

export default function TaskCard({ task, onStatusChange, onEdit, onDelete }) {
  const priority = priorityOf(task.priority)
  const category = categoryOf(task.category)
  const overdue = isOverdue(task)

  return (
    <div className="group rounded-xl border border-slate-200 bg-white p-4 shadow-sm transition hover:shadow-md">
      <div className="flex items-start justify-between gap-3">
        <div className="min-w-0 flex-1">
          <h3 className={`font-medium break-words ${task.status === 'DONE' ? 'text-slate-400 line-through' : 'text-slate-900'}`}>
            {task.title}
          </h3>
          {task.description && (
            <p className="mt-1 line-clamp-3 text-sm break-words whitespace-pre-line text-slate-500">{task.description}</p>
          )}
        </div>
        <div className="flex shrink-0 items-center gap-1">
          {category && (
            <span className={`rounded-full px-2 py-0.5 text-xs font-medium ring-1 ${category.badge}`}>
              {category.label}
            </span>
          )}
          <span className={`rounded-full px-2 py-0.5 text-xs font-medium ring-1 ${priority.badge}`}>
            {priority.label}
          </span>
        </div>
      </div>

      <div className="mt-4 flex flex-wrap items-center justify-between gap-2">
        <div className="flex items-center gap-2">
          <select
            value={task.status}
            onChange={(e) => onStatusChange(task, e.target.value)}
            className="rounded-md border border-slate-200 bg-slate-50 px-2 py-1 text-xs font-medium text-slate-700 outline-none focus:ring-2 focus:ring-indigo-100"
            aria-label="Change status"
          >
            {STATUSES.map((s) => <option key={s.value} value={s.value}>{s.label}</option>)}
          </select>
          {task.dueDate && (
            <span className={`text-xs ${overdue ? 'font-semibold text-red-600' : 'text-slate-500'}`}>
              {overdue ? 'Overdue · ' : 'Due '}{formatDate(task.dueDate)}
            </span>
          )}
        </div>
        <div className="flex gap-1">
          <button onClick={() => onEdit(task)}
            className="rounded-md px-2 py-1 text-xs font-medium text-slate-600 hover:bg-slate-100">Edit</button>
          <button onClick={() => onDelete(task)}
            className="rounded-md px-2 py-1 text-xs font-medium text-red-600 hover:bg-red-50">Delete</button>
        </div>
      </div>
    </div>
  )
}
