import { useCallback, useEffect, useMemo, useState } from 'react'
import { taskApi } from '../api.js'
import { useAuth } from '../auth/AuthContext.jsx'
import { STATUSES } from '../constants.js'
import TaskCard from '../components/TaskCard.jsx'
import TaskFormModal from '../components/TaskFormModal.jsx'
import Alert from '../components/Alert.jsx'
import Spinner from '../components/Spinner.jsx'
import NotificationBell from '../components/NotificationBell.jsx'

const FILTERS = [{ value: 'ALL', label: 'All' }, ...STATUSES]

export default function Dashboard() {
  const { user, logout } = useAuth()
  const [tasks, setTasks] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [filter, setFilter] = useState('ALL')
  const [search, setSearch] = useState('')
  const [modal, setModal] = useState(null) // null | { task: null } (new) | { task } (edit)

  const load = useCallback(async () => {
    setError('')
    try {
      setTasks(await taskApi.list())
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    load()
  }, [load])

  const counts = useMemo(() => {
    const c = { ALL: tasks.length, TODO: 0, IN_PROGRESS: 0, DONE: 0 }
    tasks.forEach((t) => { c[t.status] += 1 })
    return c
  }, [tasks])

  const visible = useMemo(() => {
    const q = search.trim().toLowerCase()
    return tasks.filter((t) =>
      (filter === 'ALL' || t.status === filter) &&
      (!q || t.title.toLowerCase().includes(q) || (t.description ?? '').toLowerCase().includes(q)))
  }, [tasks, filter, search])

  const replaceTask = (updated) => setTasks((ts) => ts.map((t) => (t.id === updated.id ? updated : t)))

  const handleSave = async (data) => {
    if (modal.task) {
      replaceTask(await taskApi.update(modal.task.id, data))
    } else {
      const created = await taskApi.create(data)
      setTasks((ts) => [created, ...ts])
    }
    setModal(null)
  }

  // Optimistic: update the UI first, roll back if the API call fails
  const handleStatusChange = async (task, status) => {
    replaceTask({ ...task, status })
    try {
      replaceTask(await taskApi.updateStatus(task.id, status))
    } catch (err) {
      replaceTask(task)
      setError(err.message)
    }
  }

  const handleDelete = async (task) => {
    if (!window.confirm(`Delete "${task.title}"?`)) return
    try {
      await taskApi.remove(task.id)
      setTasks((ts) => ts.filter((t) => t.id !== task.id))
    } catch (err) {
      setError(err.message)
    }
  }

  const progress = counts.ALL ? Math.round((counts.DONE / counts.ALL) * 100) : 0

  return (
    <div className="min-h-screen bg-slate-50">
      <header className="border-b border-slate-200 bg-white">
        <div className="mx-auto flex max-w-5xl items-center justify-between px-4 py-3">
          <div className="flex items-center gap-2">
            <img src="/favicon.svg" alt="" className="h-7 w-7" />
            <span className="text-lg font-bold text-slate-900">TaskFlow</span>
          </div>
          <div className="flex items-center gap-3">
            <NotificationBell />
            <span className="hidden text-sm text-slate-600 sm:inline">{user ? `Hi, ${user.name}` : ''}</span>
            <button onClick={logout}
              className="rounded-lg border border-slate-300 px-3 py-1.5 text-sm font-medium text-slate-700 hover:bg-slate-50">
              Log out
            </button>
          </div>
        </div>
      </header>

      <main className="mx-auto max-w-5xl px-4 py-8">
        <div className="flex flex-wrap items-end justify-between gap-4">
          <div>
            <h1 className="text-2xl font-bold text-slate-900">My tasks</h1>
            <p className="mt-1 text-sm text-slate-500">
              {counts.ALL ? `${counts.DONE} of ${counts.ALL} done · ${progress}% complete` : 'Nothing here yet'}
            </p>
          </div>
          <button onClick={() => setModal({ task: null })}
            className="rounded-lg bg-indigo-600 px-4 py-2 text-sm font-semibold text-white shadow-sm hover:bg-indigo-700">
            + New task
          </button>
        </div>

        {counts.ALL > 0 && (
          <div className="mt-4 h-2 overflow-hidden rounded-full bg-slate-200">
            <div className="h-full rounded-full bg-emerald-500 transition-all" style={{ width: `${progress}%` }} />
          </div>
        )}

        <div className="mt-6 flex flex-wrap items-center justify-between gap-3">
          <div className="flex flex-wrap gap-1 rounded-lg bg-slate-200/60 p-1">
            {FILTERS.map((f) => (
              <button key={f.value} onClick={() => setFilter(f.value)}
                className={`rounded-md px-3 py-1.5 text-sm font-medium transition ${
                  filter === f.value ? 'bg-white text-slate-900 shadow-sm' : 'text-slate-600 hover:text-slate-900'
                }`}>
                {f.label} <span className="ml-1 text-xs text-slate-400">{counts[f.value]}</span>
              </button>
            ))}
          </div>
          <input value={search} onChange={(e) => setSearch(e.target.value)} placeholder="Search tasks…"
            className="w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm outline-none focus:border-indigo-500 focus:ring-2 focus:ring-indigo-100 sm:w-64" />
        </div>

        <div className="mt-6">
          <Alert>{error}</Alert>
          {loading ? (
            <div className="flex justify-center py-16 text-indigo-600"><Spinner className="h-8 w-8" /></div>
          ) : visible.length === 0 ? (
            <div className="rounded-xl border border-dashed border-slate-300 bg-white py-16 text-center">
              <p className="font-medium text-slate-700">
                {counts.ALL === 0 ? 'No tasks yet' : 'No tasks match'}
              </p>
              <p className="mt-1 text-sm text-slate-500">
                {counts.ALL === 0 ? 'Create your first task to get started.' : 'Try a different filter or search.'}
              </p>
            </div>
          ) : (
            <div className="grid gap-3 md:grid-cols-2">
              {visible.map((task) => (
                <TaskCard key={task.id} task={task}
                  onStatusChange={handleStatusChange}
                  onEdit={(t) => setModal({ task: t })}
                  onDelete={handleDelete} />
              ))}
            </div>
          )}
        </div>
      </main>

      {modal && <TaskFormModal task={modal.task} onSave={handleSave} onClose={() => setModal(null)} />}
    </div>
  )
}
