import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Table, Typography, Tooltip, Tag } from 'antd';
import { getFeedback } from '../api/adminApi';

const { Title } = Typography;

export default function FeedbackPage() {
  const [page, setPage] = useState(1);
  const { data, isLoading } = useQuery({
    queryKey: ['admin-feedback', page],
    queryFn: () => getFeedback({ page, pageSize: 20 }),
  });

  const columns = [
    {
      title: '用户',
      dataIndex: ['user', 'email'],
      key: 'user',
      render: (_: string, r: any) => {
        if (!r.user) return <Tag color="default">已注销用户</Tag>;
        const nickname = r.user?.nickname || r.user?.displayName;
        return nickname ? `${r.user?.email}（${nickname}）` : r.user?.email || '-';
      },
    },
    {
      title: '反馈内容',
      dataIndex: 'content',
      key: 'content',
      ellipsis: true,
      render: (v: string) => (
        <Tooltip title={v} placement="topLeft">
          <span style={{ whiteSpace: 'pre-wrap' }}>{v}</span>
        </Tooltip>
      ),
    },
    {
      title: '提交时间',
      dataIndex: 'createdAt',
      key: 'createdAt',
      render: (v: string) => v ? new Date(v).toLocaleString('zh-CN') : '-',
    },
  ];

  return (
    <div>
      <Title level={4}>反馈管理</Title>
      <Table
        columns={columns}
        dataSource={data?.feedbacks}
        rowKey="id"
        loading={isLoading}
        pagination={{
          current: page,
          total: data?.total,
          pageSize: 20,
          onChange: setPage,
          showTotal: (t) => `共 ${t} 条反馈`,
        }}
      />
    </div>
  );
}
