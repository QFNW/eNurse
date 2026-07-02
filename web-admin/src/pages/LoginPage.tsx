import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Card, Form, Input, Button, Typography, message } from 'antd';
import { LockOutlined } from '@ant-design/icons';
import { adminLogin } from '../api/adminApi';

const { Title } = Typography;

export default function LoginPage() {
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();

  const onFinish = async (values: { password: string }) => {
    setLoading(true);
    try {
      const { accessToken } = await adminLogin(values.password);
      localStorage.setItem('adminToken', accessToken);
      message.success('登录成功');
      navigate('/admin');
    } catch {
      message.error('密码错误');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: '100vh', background: '#f0f2f5' }}>
      <Card style={{ width: 400 }}>
        <Title level={3} style={{ textAlign: 'center', marginBottom: 24 }}>
          易护 后台管理
        </Title>
        <Form onFinish={onFinish} layout="vertical">
          <Form.Item name="password" rules={[{ required: true, message: '请输入管理员密码' }]}>
            <Input.Password prefix={<LockOutlined />} placeholder="管理员密码" size="large" />
          </Form.Item>
          <Form.Item>
            <Button type="primary" htmlType="submit" loading={loading} block size="large">
              登录
            </Button>
          </Form.Item>
        </Form>
      </Card>
    </div>
  );
}
