import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  Table, Input, Select, Button, Space, Typography, message, Popconfirm, Modal, Form, Input as AntInput, Tag,
} from 'antd';
import { SearchOutlined, EyeOutlined } from '@ant-design/icons';
import { getUsers, updateUserRole, updateUserStatus, resetUserPassword } from '../api/adminApi';

const { Title, Text } = Typography;

export default function UsersPage() {
  const [search, setSearch] = useState('');
  const [page, setPage] = useState(1);
  const [resetTarget, setResetTarget] = useState<any>(null);
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [passwordForm] = Form.useForm();

  const { data, isLoading } = useQuery({
    queryKey: ['admin-users', search, page],
    queryFn: () => getUsers({ search, page, pageSize: 20 }),
  });

  const invalidate = () => queryClient.invalidateQueries({ queryKey: ['admin-users'] });

  const roleMutation = useMutation({
    mutationFn: ({ userId, role }: { userId: string; role: string }) =>
      updateUserRole(userId, role),
    onSuccess: () => {
      message.success('角色已更新');
      invalidate();
    },
    onError: (err: any) => {
      message.error(err?.response?.data?.message || '更新角色失败');
    },
  });

  const statusMutation = useMutation({
    mutationFn: ({ userId, isDisabled }: { userId: string; isDisabled: boolean }) =>
      updateUserStatus(userId, isDisabled),
    onSuccess: (_res, { isDisabled }) => {
      message.success(isDisabled ? '用户已禁用' : '用户已启用');
      invalidate();
    },
    onError: (err: any) => {
      message.error(err?.response?.data?.message || '操作失败');
    },
  });

  const passwordMutation = useMutation({
    mutationFn: ({ userId, newPassword }: { userId: string; newPassword: string }) =>
      resetUserPassword(userId, newPassword),
    onSuccess: () => {
      message.success('密码已重置');
      setResetTarget(null);
      passwordForm.resetFields();
    },
    onError: (err: any) => {
      message.error(err?.response?.data?.message || '重置密码失败');
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
      title: '状态', dataIndex: 'isDisabled', key: 'isDisabled',
      render: (v: boolean) => (v ? <Tag color="red">已禁用</Tag> : <Tag color="green">正常</Tag>),
    },
    {
      title: '操作', key: 'action',
      render: (_: any, record: any) => (
        <Space size={4}>
          <Button
            size="small"
            icon={<EyeOutlined />}
            onClick={() => navigate(`/admin/users/${record.id}`)}
          >
            详情
          </Button>
          <Popconfirm
            title={record.isDisabled ? '确认启用此用户？' : '确认禁用此用户？禁用后用户将无法登录'}
            onConfirm={() => statusMutation.mutate({ userId: record.id, isDisabled: !record.isDisabled })}
          >
            <Button size="small" danger={!record.isDisabled}>
              {record.isDisabled ? '启用' : '禁用'}
            </Button>
          </Popconfirm>
          <Button size="small" onClick={() => { setResetTarget(record); passwordForm.resetFields(); }}>
            重置密码
          </Button>
        </Space>
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

      <Modal
        title={`重置密码${resetTarget ? ` - ${resetTarget.email}` : ''}`}
        open={!!resetTarget}
        onOk={() => passwordForm.submit()}
        onCancel={() => setResetTarget(null)}
        confirmLoading={passwordMutation.isPending}
        okText="确认重置"
        cancelText="取消"
      >
        <Text type="secondary" style={{ display: 'block', marginBottom: 16 }}>
          将直接设置该用户的新密码，原密码立即失效。
        </Text>
        <Form
          form={passwordForm}
          layout="vertical"
          onFinish={(values) =>
            resetTarget &&
            passwordMutation.mutate({ userId: resetTarget.id, newPassword: values.newPassword })
          }
        >
          <Form.Item
            name="newPassword"
            label="新密码"
            rules={[
              { required: true, message: '请输入新密码' },
              { min: 6, message: '密码至少 6 位' },
            ]}
          >
            <AntInput.Password placeholder="至少 6 位" autoComplete="new-password" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
