import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Table, Typography } from 'antd';
import { getGroups } from '../api/adminApi';

const { Title } = Typography;

export default function GroupsPage() {
  const [page, setPage] = useState(1);
  const { data, isLoading } = useQuery({
    queryKey: ['admin-groups', page],
    queryFn: () => getGroups({ page, pageSize: 20 }),
  });

  const columns = [
    { title: '小组名称', dataIndex: 'name', key: 'name' },
    { title: '邀请码', dataIndex: 'inviteCode', key: 'inviteCode' },
    { title: '创建者', dataIndex: ['creator', 'email'], key: 'creator' },
    { title: '成员数', dataIndex: ['_count', 'members'], key: 'members' },
    {
      title: '创建时间',
      dataIndex: 'createdAt',
      key: 'createdAt',
      render: (v: string) => v ? new Date(v).toLocaleDateString('zh-CN') : '-',
    },
  ];

  return (
    <div>
      <Title level={4}>小组管理</Title>
      <Table
        columns={columns}
        dataSource={data?.groups}
        rowKey="id"
        loading={isLoading}
        pagination={{
          current: page,
          total: data?.total,
          pageSize: 20,
          onChange: setPage,
          showTotal: (t) => `共 ${t} 个小组`,
        }}
      />
    </div>
  );
}
