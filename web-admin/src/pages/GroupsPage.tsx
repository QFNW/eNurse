import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  Table, Typography, Button, Space, message, Popconfirm, Modal, Tag, Spin, Empty,
} from 'antd';
import { UsergroupAddOutlined, DeleteOutlined } from '@ant-design/icons';
import { getGroups, getGroupMembers, deleteGroup } from '../api/adminApi';

const { Title } = Typography;

const ROLE_LABEL: Record<string, string> = {
  OWNER: '组长',
  ADMIN: '管理员',
  MEMBER: '成员',
};

export default function GroupsPage() {
  const [page, setPage] = useState(1);
  const [memberTarget, setMemberTarget] = useState<any>(null);
  const queryClient = useQueryClient();
  const { data, isLoading } = useQuery({
    queryKey: ['admin-groups', page],
    queryFn: () => getGroups({ page, pageSize: 20 }),
  });

  const membersQuery = useQuery({
    queryKey: ['admin-group-members', memberTarget?.id],
    queryFn: () => getGroupMembers(memberTarget.id),
    enabled: !!memberTarget,
  });

  const deleteMutation = useMutation({
    mutationFn: (groupId: string) => deleteGroup(groupId),
    onSuccess: (_res, groupId) => {
      message.success('小组已解散');
      queryClient.invalidateQueries({ queryKey: ['admin-groups'] });
      setMemberTarget((cur: any) => (cur?.id === groupId ? null : cur));
    },
    onError: (err: any) => {
      message.error(err?.response?.data?.message || '解散小组失败');
    },
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
    {
      title: '操作',
      key: 'action',
      render: (_: any, record: any) => (
        <Space size={4}>
          <Button
            size="small"
            icon={<UsergroupAddOutlined />}
            onClick={() => setMemberTarget(record)}
          >
            成员
          </Button>
          <Popconfirm
            title="解散小组"
            description={`确认解散「${record.name}」？小组及全部成员关系将被删除。`}
            okText="确认解散"
            okButtonProps={{ danger: true }}
            onConfirm={() => deleteMutation.mutate(record.id)}
          >
            <Button size="small" danger icon={<DeleteOutlined />}>
              解散
            </Button>
          </Popconfirm>
        </Space>
      ),
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

      <Modal
        title={memberTarget ? `成员列表 - ${memberTarget.name}` : '成员列表'}
        open={!!memberTarget}
        onCancel={() => setMemberTarget(null)}
        footer={<Button onClick={() => setMemberTarget(null)}>关闭</Button>}
      >
        {membersQuery.isLoading ? (
          <div style={{ textAlign: 'center', padding: 24 }}>
            <Spin />
          </div>
        ) : membersQuery.data?.members?.length ? (
          <Table
            rowKey="id"
            size="small"
            pagination={false}
            dataSource={membersQuery.data.members}
            columns={[
              {
                title: '姓名',
                dataIndex: ['user', 'displayName'],
                key: 'name',
                render: (_: string, r: any) => r.user?.displayName || '-',
              },
              {
                title: '邮箱',
                dataIndex: ['user', 'email'],
                key: 'email',
                render: (v: string) => v || '-',
              },
              {
                title: '角色',
                dataIndex: 'roleInGroup',
                key: 'roleInGroup',
                render: (v: string) => (
                  <Tag color={v === 'OWNER' ? 'gold' : v === 'ADMIN' ? 'blue' : 'default'}>
                    {ROLE_LABEL[v] || v}
                  </Tag>
                ),
              },
              {
                title: '加入时间',
                dataIndex: 'joinedAt',
                key: 'joinedAt',
                render: (v: string) => v ? new Date(v).toLocaleDateString('zh-CN') : '-',
              },
            ]}
          />
        ) : (
          <Empty description="暂无成员" />
        )}
      </Modal>
    </div>
  );
}
