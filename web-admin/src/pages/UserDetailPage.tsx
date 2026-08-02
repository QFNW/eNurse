import { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  Descriptions, Card, Table, Button, Space, Tag, Typography, message, Popconfirm, Modal, Form, Input,
} from 'antd';
import { ArrowLeftOutlined } from '@ant-design/icons';
import { getUserDetail, updateUserStatus, resetUserPassword } from '../api/adminApi';

const { Title, Text } = Typography;

const ROLE_LABEL: Record<string, string> = {
  PATIENT: '患者',
  NURSE: '护理',
  ADMIN: '管理员',
};

export default function UserDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [passwordModalOpen, setPasswordModalOpen] = useState(false);
  const [passwordForm] = Form.useForm();

  const { data, isLoading, isError } = useQuery({
    queryKey: ['admin-user', id],
    queryFn: () => getUserDetail(id!),
    enabled: !!id,
  });

  const invalidate = () => {
    queryClient.invalidateQueries({ queryKey: ['admin-user', id] });
    queryClient.invalidateQueries({ queryKey: ['admin-users'] });
  };

  const statusMutation = useMutation({
    mutationFn: (isDisabled: boolean) => updateUserStatus(id!, isDisabled),
    onSuccess: (_res, isDisabled) => {
      message.success(isDisabled ? '用户已禁用' : '用户已启用');
      invalidate();
    },
  });

  const passwordMutation = useMutation({
    mutationFn: (newPassword: string) => resetUserPassword(id!, newPassword),
    onSuccess: () => {
      message.success('密码已重置');
      setPasswordModalOpen(false);
      passwordForm.resetFields();
    },
    onError: (err: any) => {
      message.error(err?.response?.data?.message || '重置密码失败');
    },
  });

  const handleResetPassword = (values: { newPassword: string }) => {
    passwordMutation.mutate(values.newPassword);
  };

  const bmColumns = [
    { title: '记录时间', dataIndex: 'recordedAt', key: 'recordedAt', render: (v: string) => new Date(v).toLocaleString('zh-CN') },
    { title: '备注', dataIndex: 'notes', key: 'notes', render: (v: string | null) => v || '-' },
  ];

  const medColumns = [
    { title: '时间', dataIndex: 'takenAt', key: 'takenAt', render: (v: string) => new Date(v).toLocaleString('zh-CN') },
    { title: '药品', key: 'medication', render: (_: any, r: any) => r.medication?.name || '-' },
    { title: '备注', dataIndex: 'notes', key: 'notes', render: (v: string | null) => v || '-' },
  ];

  const groupColumns = [
    { title: '小组名称', dataIndex: 'name', key: 'name' },
    { title: '角色', dataIndex: 'roleInGroup', key: 'roleInGroup', render: (v: string) => v || 'MEMBER' },
    { title: '成员数', dataIndex: 'memberCount', key: 'memberCount' },
    { title: '加入时间', dataIndex: 'joinedAt', key: 'joinedAt', render: (v: string) => new Date(v).toLocaleString('zh-CN') },
  ];

  return (
    <div>
      <Space style={{ marginBottom: 16 }}>
        <Button icon={<ArrowLeftOutlined />} onClick={() => navigate('/admin/users')}>返回列表</Button>
        <Title level={4} style={{ margin: 0 }}>用户详情</Title>
      </Space>

      {!data && isLoading && <Card loading style={{ marginBottom: 16 }} />}
      {isError && (
        <Card><Text type="danger">加载失败：用户不存在或网络错误</Text></Card>
      )}

      {data && (
        <>
          <Card loading={isLoading} style={{ marginBottom: 16 }}>
            <Descriptions column={3} bordered size="small">
              <Descriptions.Item label="邮箱">{data.email}</Descriptions.Item>
              <Descriptions.Item label="用户名">{data.username}</Descriptions.Item>
              <Descriptions.Item label="昵称">{data.nickname || '-'}</Descriptions.Item>
              <Descriptions.Item label="角色">{ROLE_LABEL[data.role] || data.role}</Descriptions.Item>
              <Descriptions.Item label="状态">
                {data.isDisabled
                  ? <Tag color="red">已禁用</Tag>
                  : <Tag color="green">正常</Tag>}
              </Descriptions.Item>
              <Descriptions.Item label="虚拟账号">{data.isVirtual ? '是' : '否'}</Descriptions.Item>
              <Descriptions.Item label="注册时间">{new Date(data.createdAt).toLocaleString('zh-CN')}</Descriptions.Item>
              <Descriptions.Item label="大便打卡">{data.counts?.bmRecords ?? 0} 条</Descriptions.Item>
              <Descriptions.Item label="用药打卡">{data.counts?.medicationRecords ?? 0} 条</Descriptions.Item>
            </Descriptions>
            <Space style={{ marginTop: 16 }}>
              <Popconfirm
                title={data.isDisabled ? '确认启用此用户？' : '确认禁用此用户？禁用后用户将无法登录'}
                onConfirm={() => statusMutation.mutate(!data.isDisabled)}
              >
                <Button danger={!data.isDisabled} loading={statusMutation.isPending}>
                  {data.isDisabled ? '启用用户' : '禁用用户'}
                </Button>
              </Popconfirm>
              <Button onClick={() => { setPasswordModalOpen(true); passwordForm.resetFields(); }}>重置密码</Button>
            </Space>
          </Card>

          <Card title={`所属小组（${data.groups?.length ?? 0}）`} style={{ marginBottom: 16 }}>
            <Table columns={groupColumns} dataSource={data.groups || []} rowKey="id" size="small" pagination={false} />
          </Card>

          <Space direction="vertical" size={16} style={{ display: 'flex' }}>
            <Card title={`大便打卡（最近 ${data.bmRecords?.length ?? 0} 条）`}>
              <Table columns={bmColumns} dataSource={data.bmRecords || []} rowKey="id" size="small" pagination={false} />
            </Card>
            <Card title={`用药打卡（最近 ${data.medicationRecords?.length ?? 0} 条）`}>
              <Table columns={medColumns} dataSource={data.medicationRecords || []} rowKey="id" size="small" pagination={false} />
            </Card>
            {data.medications?.length > 0 && (
              <Card title={`常用药品（${data.medications.length}）`}>
                <Space wrap>
                  {data.medications.map((m: any) => (
                    <Tag key={m.id} color="blue">
                      {m.name}{m.dosage ? ` ${m.dosage}${m.unit || ''}` : ''}
                    </Tag>
                  ))}
                </Space>
              </Card>
            )}
          </Space>
        </>
      )}

      <Modal
        title="重置密码"
        open={passwordModalOpen}
        onOk={() => passwordForm.submit()}
        onCancel={() => setPasswordModalOpen(false)}
        confirmLoading={passwordMutation.isPending}
        okText="确认重置"
        cancelText="取消"
      >
        <Text type="secondary" style={{ display: 'block', marginBottom: 16 }}>
          将直接设置该用户的新密码，原密码立即失效。
        </Text>
        <Form form={passwordForm} layout="vertical" onFinish={handleResetPassword}>
          <Form.Item
            name="newPassword"
            label="新密码"
            rules={[
              { required: true, message: '请输入新密码' },
              { min: 6, message: '密码至少 6 位' },
            ]}
          >
            <Input.Password placeholder="至少 6 位" autoComplete="new-password" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
