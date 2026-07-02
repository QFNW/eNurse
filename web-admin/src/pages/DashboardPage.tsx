import { useQuery } from '@tanstack/react-query';
import { Row, Col, Card, Statistic, Typography } from 'antd';
import { UserOutlined, TeamOutlined, AlertOutlined, CheckCircleOutlined } from '@ant-design/icons';
import { getStats } from '../api/adminApi';

const { Title } = Typography;

export default function DashboardPage() {
  const { data, isLoading } = useQuery({ queryKey: ['admin-stats'], queryFn: getStats });

  return (
    <div>
      <Title level={4}>系统概览</Title>
      <Row gutter={[16, 16]}>
        <Col xs={24} sm={12} lg={6}>
          <Card loading={isLoading}>
            <Statistic title="总用户" value={data?.users?.total ?? 0} prefix={<UserOutlined />} />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card loading={isLoading}>
            <Statistic title="患者" value={data?.users?.patients ?? 0} prefix={<UserOutlined />} valueStyle={{ color: '#1677ff' }} />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card loading={isLoading}>
            <Statistic title="护理人员" value={data?.users?.nurses ?? 0} prefix={<TeamOutlined />} valueStyle={{ color: '#52c41a' }} />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card loading={isLoading}>
            <Statistic title="小组数" value={data?.groups ?? 0} prefix={<TeamOutlined />} />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card loading={isLoading}>
            <Statistic title="今日大便打卡" value={data?.records?.bmToday ?? 0} prefix={<CheckCircleOutlined />} valueStyle={{ color: '#1677ff' }} />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card loading={isLoading}>
            <Statistic title="大便总记录" value={data?.records?.bmTotal ?? 0} />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card loading={isLoading}>
            <Statistic title="今日用药打卡" value={data?.records?.medToday ?? 0} prefix={<CheckCircleOutlined />} valueStyle={{ color: '#52c41a' }} />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card loading={isLoading} style={{ borderColor: data?.alerts?.active > 0 ? '#ff4d4f' : undefined }}>
            <Statistic
              title="活跃警报"
              value={data?.alerts?.active ?? 0}
              prefix={<AlertOutlined />}
              valueStyle={{ color: data?.alerts?.active > 0 ? '#ff4d4f' : '#52c41a' }}
            />
          </Card>
        </Col>
      </Row>
    </div>
  );
}
