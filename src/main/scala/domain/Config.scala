package domain

case class GroupingConfigs(windowCount: Int, windowSeconds: Int)
object GroupingConfigs {
  def default = GroupingConfigs(50, 15)
}
