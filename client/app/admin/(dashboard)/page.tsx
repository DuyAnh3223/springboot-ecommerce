import React from 'react'
import WelcomeBanner from '../components/WelcomeBanner'
import StatsGrid from '../components/StatsGrid'
import RecentActivity from '../components/RecentActivity'
import QuickLinks from '../components/QuickLinks'

export default function AdminDashboardPage() {
  return (
    <div className="space-y-3 animate-in fade-in duration-300">

      {/* Stats Grid */}
      <StatsGrid />

      {/* Quick Info & Logs */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Recent Activity Card */}
        <div className="col-span-1 lg:col-span-2">
          <RecentActivity />
        </div>

        {/* Quick Links Card */}
        <QuickLinks />
      </div>
    </div>
  )
}
