/** 跑步活动 */
export interface RunningActivity {
  id: number
  userId: number
  platform: string
  activityName: string
  activityType: string
  startTime: string
  endTime: string
  durationSeconds: number
  distanceM: number
  avgPaceSecKm: number
  maxPaceSecKm: number
  avgHeartRate: number | null
  maxHeartRate: number | null
  avgCadence: number | null
  calories: number | null
  elevationGainM: number | null
  remark: string | null
  createTime: string
}

/** 跑步分段 */
export interface RunningLap {
  id: number
  activityId: number
  lapIndex: number
  splitDistanceM: number
  splitDurationSec: number
  avgPaceSecKm: number
  avgHeartRate: number | null
}

/** 跑步详情 VO */
export interface ActivityDetailVO {
  activity: RunningActivity
  laps: RunningLap[]
  distanceText: string
  durationText: string
  paceText: string
  caloriesText: string
}

/** 周统计 */
export interface WeeklyStats {
  totalActivities: number
  totalDistanceM: number
  totalDurationSec: number
  totalCalories: number
  avgPace: number
  avgHeartRate: number
}

/** 新增跑步记录 */
export interface ActivityCreateForm {
  activityName?: string
  activityType?: string
  startTime: string
  endTime: string
  durationSeconds: number
  distanceM: number
  avgHeartRate?: number
  maxHeartRate?: number
  avgCadence?: number
  calories?: number
  elevationGainM?: number
  remark?: string
}

/** 查询参数 */
export interface ActivityQuery {
  pageNum: number
  pageSize: number
  startDate?: string
  endDate?: string
  activityType?: string
}
