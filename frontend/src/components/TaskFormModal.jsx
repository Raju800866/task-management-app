import { useEffect, useState } from 'react'
import { CATEGORIES, PRIORITIES, STATUSES } from '../constants.js'
import Alert from './Alert.jsx'
import Spinner from './Spinner.jsx'

const EMPTY = { title: '', description: '', status: 'TODO', priority: 'MEDIUM', category: '', dueDate: '' }

export default function TaskFormModal({ task, onSave, onClose }) {
  const [form, setForm] = useState(EMPTY)
  const [error, setError] = useState('')
  const [fieldErrors, setFieldErrors] = useState({})
  const [saving, setSaving] = useState(false)

  useEffect(() => {
    setForm(task
      ? {
          title: task.title,
          description: task.description ?? '',
          status: task.status,
          priority: task.priority,
          category: task.category ?? '',
          dueDate: task.dueDate ?? '',
        }
      : EMPTY)
  }, [task])

  // Close on Escape
  useEffect(() => {
    const onKey = (e) => e.key === 'Escape' && onClose()
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [onClose])

  const onChange = (e) => setForm({ ...form, [e.target.name]: e.target.value })

  const onSubmit = async (e) => {
    e.preventDefault()
    if (!form.title.trim()) {
      setFieldErrors({ title: 'Title is required' })
      return
    }
    setSaving(true)
    setError('')
    try {
      await onSave({
        title: form.title.trim(),
        description: form.description.trim() || null,
        status: form.status,
        priority: form.priority,
        category: form.category || null,
        dueDate: form.dueDate || null,
      })
    } catch (err) {
      setFieldErrors(err.fieldErrors || {})
      setError(err.message)
      setSaving(false)
    }
  }

  const inputCls = 'w-full rounded-lg border border-slate-300 px-3 py-2 text-sm outline-none focus:border-indigo-500 focus:ring-2 focus:ring-indigo-100'

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/40 p-4" onMouseDown={onClose}>
      <div className="w-full max-w-lg rounded-2xl bg-white p-6 shadow-xl" onMouseDown={(e) => e.stopPropagation()}>
        <h2 className="text-lg font-semibold text-slate-900">{task ? 'Edit task' : 'New task'}</h2>
        <form onSubmit={onSubmit} className="mt-4 space-y-4" noValidate>
          <Alert>{error}</Alert>
          <label className="block">
            <span className="mb-1 block text-sm font-medium text-slate-700">Title</span>
            <input name="title" value={form.title} onChange={onChange} maxLength={200} autoFocus
              className={`${inputCls} ${fieldErrors.title ? 'border-red-400' : ''}`} />
            {fieldErrors.title && <span className="mt-1 block text-xs text-red-600">{fieldErrors.title}</span>}
          </label>
          <label className="block">
            <span className="mb-1 block text-sm font-medium text-slate-700">Description</span>
            <textarea name="description" value={form.description} onChange={onChange} rows={3} maxLength={2000}
              className={inputCls} />
          </label>
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <label className="block">
              <span className="mb-1 block text-sm font-medium text-slate-700">Status</span>
              <select name="status" value={form.status} onChange={onChange} className={inputCls}>
                {STATUSES.map((s) => <option key={s.value} value={s.value}>{s.label}</option>)}
              </select>
            </label>
            <label className="block">
              <span className="mb-1 block text-sm font-medium text-slate-700">Priority</span>
              <select name="priority" value={form.priority} onChange={onChange} className={inputCls}>
                {PRIORITIES.map((p) => <option key={p.value} value={p.value}>{p.label}</option>)}
              </select>
            </label>
            <label className="block">
              <span className="mb-1 block text-sm font-medium text-slate-700">Category</span>
              <select name="category" value={form.category} onChange={onChange} className={inputCls}>
                <option value="">None</option>
                {CATEGORIES.map((c) => <option key={c.value} value={c.value}>{c.label}</option>)}
              </select>
            </label>
            <label className="block">
              <span className="mb-1 block text-sm font-medium text-slate-700">Due date</span>
              <input type="date" name="dueDate" value={form.dueDate} onChange={onChange} className={inputCls} />
            </label>
          </div>
          <div className="flex justify-end gap-2 pt-2">
            <button type="button" onClick={onClose}
              className="rounded-lg px-4 py-2 text-sm font-medium text-slate-600 hover:bg-slate-100">Cancel</button>
            <button type="submit" disabled={saving}
              className="flex items-center gap-2 rounded-lg bg-indigo-600 px-4 py-2 text-sm font-semibold text-white hover:bg-indigo-700 disabled:opacity-60">
              {saving && <Spinner />} {task ? 'Save changes' : 'Create task'}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}
