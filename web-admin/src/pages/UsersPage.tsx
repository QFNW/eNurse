import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Table, Input, Select, Button, Space, Typography, message, Popconfirm } from 'antd';
import { SearchOutlined } from '@ant-design/icons';
import { getUsers, updateUserRole, disableUser } from '../api/adminApi';

const { Title } = Typography;

export default function UsersPage() {
  const [search, setSearch] = useState('');
  const [page, setPage] = useState(1);
  const queryClient = useQueryClient();

  const { data, isLoading } = useQuery({
    queryKey: ['admin-users', search, page],
    queryFn: () => getUsers({ search, page, pageSize: 20 }),
  });

  const roleMutation = useMutation({
    mutationFn: ({ userId, role }: { userId: string; role: string }) =>
      updateUserRole(userId, role),
    onSuccess: () => {
      message.success('角色已更新');
      queryClient.invalidateQueries({ queryKey: ['admin-users'] });
    },
  });

  const disableMutation = useMutation({
    mutationFn: (userId: string) => disableUser(userId),
    onSuccess: () => {
      message.success('用户已禁用');
      queryClient.invalidateQueries({ queryKey: ['admin-users'] });
    },
  });

  const columns = [
    { title: '邮箱', dataIndex: 'email', key: 'email' },
    { title: '昵称', dataIndex: 'nickname', key: 'nickname', render: (v: string | null) => v || '-' },
    {
      title: '角色', dataIndex: 'role', key: 'role',
      render: (_: string, record: any) => (
        <Select
          value={record.role}
          style={{ width: 100 }}
          onChange={(role: string) => roleMutation.mutate({ userId: record.id, role })}
          options={[
            { value: 'PATIENT', label: '患者' },
            { value: 'NURSE', label: '护理' },
            { value: 'ADMIN', label: '管理员' },
          ]}
        />
      ),
    },
    {
      title: '操作', key: 'action',
      render: (_: any, record: any) => (
        <Popconfirm title="确认禁用此用户？" onConfirm={() => disableMutation.mutate(record.id)}>
          <Button danger size="small">禁用</Button>
        </Popconfirm>
      ),
    },
  ];

  return (
    <div>
      <Title level={4}>用户管理</Title>
      <Space style={{ marginBottom: 16 }}>
        <Input
          prefix={<SearchOutlined />}
          placeholder="搜索邮箱或昵称"
          value={search}
          onChange={(e) => { setSearch(e.target.value); setPage(1); }}
          style={{ width: 240 }}
        />
      </Space>
      <Table
        columns={columns}
        dataSource={data?.users}
        rowKey="id"
        loading={isLoading}
        pagination={{
          current: page,
          total: data?.total,
          pageSize: 20,
          onChange: setPage,
          showTotal: (t) => `共 ${t} 个用户`,
        }}
      />
    </div>
  );
}
